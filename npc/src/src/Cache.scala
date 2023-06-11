import chisel3._
import chisel3.util._
import chisel3.util.random._

class Cache_Sram(width : Int, depth : Int) extends Module
{
    val io = IO(new Bundle
    {
        val Q    = Output(UInt(width.W))
        val cen  = Input(Bool())
        val wen  = Input(Bool())
        val bwen = Input(UInt(width.W))
        val A    = Input(UInt(log2Ceil(depth).W))
        val D    = Input(UInt(width.W))
    })

    val ram = RegInit(VecInit(Seq.fill(depth)(0.U(width.W))))
    ram(io.A) := RegEnable((io.D & io.bwen) | (ram(io.A) & ~io.bwen), io.cen && io.wen)
    io.Q := RegEnable(ram(io.A), io.cen && !io.wen)
}

class Bank_IO extends Bundle
{
    val Q    = Output(UInt(64.W))
    val cen  = Input(Bool())
    val wen  = Input(Bool())
    val bwen = Input(UInt(64.W))
    val A    = Input(UInt(7.W))
    val D    = Input(UInt(64.W))
}

class Cache_Line extends Module
{
    val banks = Seq.fill(4)(IO(new Bank_IO))
    val datas = Seq.fill(4)(Module(new Cache_Sram(64, 128)))

    for (i <- 0 until 4)
        datas(i).io <> banks(i)
}

class Cache_Way extends Bundle
{
    val tag  = Module(new Cache_Sram(52, 128))
    val V    = Module(new Cache_Sram(1, 128))
    val D    = Module(new Cache_Sram(1, 128))
    val data = Module(new Cache_Line)
}

class Cache_Req extends Bundle
{
    val valid  = Bool()
    val op     = Bool()
    val tag    = UInt(52.W)
    val index  = UInt(7.W)
    val offset = UInt(5.W)
}

class Cache(way : Int) extends Module
{
    val cpu_master = IO(Flipped(new AXI_Lite_Master))
    val cpu_slave  = IO(Flipped(new AXI_Lite_Slave))
    val master     = IO(new AXI_Lite_Master)
    val slave      = IO(new AXI_Lite_Slave)

    val ways = Seq.fill(way)(new Cache_Way)
    val random_bit = LFSR(16)

    val req = Wire(new Cache_Req)
    req.valid  := cpu_master.ar.valid || cpu_master.aw.valid
    req.op     := cpu_master.aw.valid
    req.tag    := Mux(req.op, cpu_master.aw.bits.addr(63, 13), cpu_master.ar.bits.addr(63, 13))
    req.index  := Mux(req.op, cpu_master.aw.bits.addr(12, 5), cpu_master.ar.bits.addr(12, 5))
    req.offset := Mux(req.op, cpu_master.aw.bits.addr(4, 0), cpu_master.ar.bits.addr(4, 0))

    val s_idle :: s_lookup :: s_aw :: s_w :: s_ar :: s_r :: Nil = Enum(6)
    val state = RegInit(s_idle)

    val hazard = Wire(Bool())
    val hit = Wire(Bool())
    val cnt = RegInit(0.U(2.W))

    state := MuxLookup(state, s_idle, Seq(
        s_idle   -> Mux(req.valid && !hazard, s_lookup, s_idle),
        // what if cpu is not ready and cache has to wait for bready?
        s_lookup -> Mux(hit, Mux(req.valid && !hazard, s_lookup, s_idle), s_aw),
        s_aw     -> Mux(master.aw.fire, s_w, s_aw),
        s_w      -> Mux(slave.b.fire, Mux(cnt === 2.U, s_ar, s_aw), s_w),
        s_ar     -> Mux(master.ar.fire, s_r, s_ar),
        s_r      -> Mux(slave.r.fire, Mux(cnt === 2.U, s_idle, s_r), s_r)
    ))

    val req_reg = RegEnable(req, (state === s_idle || (state === s_lookup && hit)) && req.valid && !hazard)

    val hit_way = Seq.fill(way)(Wire(Bool()))
    // val cache_line = Seq.fill(way)(Wire(UInt(256.W)))
    val cache_line = Seq.fill(way)(Wire(Vec(4, UInt(64.W))))

    // for (i <- 0 until way)
    //     cache_line(i) := DontCare

    cpu_slave.r.bits.data := 0.U(64.W)

    for (i <- 0 until way)
    {
        ways(i).tag.io.cen  := req.valid
        ways(i).tag.io.wen  := DontCare
        ways(i).tag.io.bwen := DontCare
        ways(i).tag.io.A    := req.index
        ways(i).tag.io.D    := DontCare

        ways(i).V.io.cen   := req.valid
        ways(i).V.io.wen   := DontCare
        ways(i).V.io.bwen  := DontCare
        ways(i).V.io.A     := req.index
        ways(i).V.io.D     := DontCare

        ways(i).D.io.cen   := req.valid
        ways(i).D.io.wen   := DontCare
        ways(i).D.io.bwen  := DontCare
        ways(i).D.io.A     := req.index
        ways(i).D.io.D     := DontCare

        for (j <- 0 until 4)
        {
            ways(i).data.banks(j).cen  := req.valid
            ways(i).data.banks(j).wen  := DontCare
            ways(i).data.banks(j).bwen := DontCare
            ways(i).data.banks(j).A    := req.index
            ways(i).data.banks(j).D    := DontCare
            cache_line(i)(j * 64, (j + 1) * 64 - 1) := ways(i).data.banks(j).Q
        }

        hit_way(i) := ways(i).V.io.Q === 1.U && ways(i).tag.io.Q === req_reg.tag

        when (state === s_lookup && hit_way(i))
        {
            cpu_slave.r.bits.data := cache_line(i)(req_reg.offset(4, 2))
        }
    }

    hit := hit_way.reduce(_ || _)

    val ret_data_reg = Wire(UInt(256.W))
    ret_data_reg := DontCare

    val cache_ready = (state === s_idle || (state === s_lookup && hit)) && req.valid && !hazard
    
    cpu_master.aw.ready := cache_ready
    cpu_master.w.ready  := cache_ready
    cpu_master.ar.ready := cache_ready

    cpu_slave.b.valid := (state === s_lookup && hit) || (state === s_r && slave.r.fire && cnt === 2.U) && req_reg.op
    cpu_slave.b.bits.resp := 0.U(2.W)

    cpu_slave.r.valid := (state === s_lookup && hit) || (state === s_r && slave.r.fire && cnt === 2.U) && !req_reg.op
    cpu_slave.r.bits.resp := 0.U(2.W)
    when (state === s_r)
    {
        cpu_slave.r.bits.data := ret_data_reg(req_reg.offset(4, 2))
    }

    master.aw.valid := state === s_aw
    master.aw.bits.addr := DontCare
    master.aw.bits.prot := 0.U(3.W)

    master.w.valid := state === s_w
    master.w.bits.data := DontCare
    master.w.bits.strb := Fill(8, 1.U)

    master.ar.valid := state === s_ar
    master.ar.bits.addr := DontCare
    master.ar.bits.prot := 0.U(3.W)

    slave.b.ready := state === s_w
    slave.r.ready := state === s_r

    hazard := DontCare
}