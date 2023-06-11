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

class Cache(way : Int) extends Module
{
    val cpu_master = IO(Flipped(new AXI_Lite_Master))
    val cpu_slave  = IO(Flipped(new AXI_Lite_Slave))
    val master     = IO(new AXI_Lite_Master)
    val slave      = IO(new AXI_Lite_Slave)

    val ways = Seq.fill(way)(new Cache_Way)
    val random_bit = LFSR(2)

    val req = new Bundle
    {
        val valid  = cpu_master.ar.valid || cpu_master.aw.valid
        val op     = cpu_master.aw.valid
        val tag    = Mux(op, cpu_master.aw.bits.addr(63, 13), cpu_master.ar.bits.addr(63, 13))
        val index  = Mux(op, cpu_master.aw.bits.addr(12, 5), cpu_master.ar.bits.addr(12, 5))
        val offset = Mux(op, cpu_master.aw.bits.addr(4, 0), cpu_master.ar.bits.addr(4, 0))
    }

    val s_idle :: s_lookup :: s_aw :: s_w :: s_ar :: s_r :: Nil = Enum(5)
    val state = RegInit(s_idle)

    val hazard = Wire(Bool())
    val hit = Wire(Bool())
    val write_finish = Wire(Bool())
    val read_finish = Wire(Bool())
    val cnt = RegInit(0.U(2.W))

    state := MuxLookup(state, s_idle, Seq(
        s_idle   -> Mux(req.valid && !hazard, s_lookup, s_idle),
        s_lookup -> Mux(hit, Mux(req.valid && !hazard, s_lookup, s_idle), s_aw),
        s_aw     -> Mux(master.aw.fire, s_w, s_aw),
        s_w      -> Mux(slave.b.fire, Mux(cnt === 2.U, s_ar, s_aw), s_w),
        s_ar     -> Mux(master.ar.fire, s_r, s_ar),
        s_r      -> Mux(slave.r.fire, Mux(cnt === 2.U, s_idle, s_r), s_r)
    ))

    val req_reg = RegEnable(req, (state === s_idle || (state === s_lookup && hit)) && req.valid && !hazard)

    val hit_way = Seq.fill(way)(Wire(Bool()))
    val cache_line = Seq.fill(way)(Wire(UInt(256.W)))

    for (i <- 0 until 2)
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
            ways(i).data.banks(j).A    := req.offset
            ways(i).data.banks(j).D    := DontCare
        }

        hit_way(i) := ways(i).V.io.Q === 1.U && ways(i).tag.io.Q === req_reg.tag
        hit := hit_way(i) || hit

        when (state === s_lookup && hit_way(i))
        {
            cpu_slave.r.bits.data := cache_line(i)(req_reg.offset(4, 2))
        }
    }

    val ret_data_reg = Wire(UInt(256.W))

    when (state === s_r)
    {
        cpu_slave.r.bits.data := ret_data_reg(req_reg.offset(4, 2))
    }
}