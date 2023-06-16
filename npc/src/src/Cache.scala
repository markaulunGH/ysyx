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
    val datas = Seq.fill(4)(Module(new Cache_Sram(64, 64)))

    for (i <- 0 until 4)
    {
        datas(i).io <> banks(i)
    }
}

class Cache_Way extends Bundle
{
    val tag  = Module(new Cache_Sram(53, 128))
    val V    = Module(new Cache_Sram(1, 128))
    val D    = Module(new Cache_Sram(1, 128))
    val data = Module(new Cache_Line)
}

class Cache_Req extends Bundle
{
    val valid  = Bool()
    val op     = Bool()
    val tag    = UInt(53.W)
    val index  = UInt(6.W)
    val offset = UInt(5.W)
    val data   = UInt(64.W)
    val strb   = UInt(8.W)
}

class Cache(way : Int) extends Module
{
    val cpu_master = IO(Flipped(new AXI_Lite_Master))
    val cpu_slave  = IO(Flipped(new AXI_Lite_Slave))
    val master     = IO(new AXI_Lite_Master)
    val slave      = IO(new AXI_Lite_Slave)

    val ways = Seq.fill(way)(new Cache_Way)
    val random_bit = LFSR(16)

    val req = dontTouch(Wire(new Cache_Req))
    req.valid  := cpu_master.ar.valid || cpu_master.aw.valid
    req.op     := cpu_master.aw.valid
    req.tag    := Mux(req.op, cpu_master.aw.bits.addr(63, 11), cpu_master.ar.bits.addr(63, 11))
    req.index  := Mux(req.op, cpu_master.aw.bits.addr(10, 5), cpu_master.ar.bits.addr(10, 5))
    req.offset := Mux(req.op, cpu_master.aw.bits.addr(4, 0), cpu_master.ar.bits.addr(4, 0))
    req.data   := cpu_master.w.bits.data
    req.strb   := cpu_master.w.bits.strb

    val s_idle :: s_lookup :: s_miss :: s_aw :: s_w :: s_ar :: s_r :: Nil = Enum(7)
    val state = RegInit(s_idle)

    val dirty = dontTouch(Wire(Bool()))
    val hazard = dontTouch(Wire(Bool()))
    val hit = dontTouch(Wire(Bool()))
    val cnt = RegInit(0.U(2.W))

    state := MuxLookup(state, s_idle, Seq(
        s_idle   -> Mux(req.valid && !hazard, s_lookup, s_idle),
        // what if cpu is not ready and cache has to wait for bready?
        s_lookup -> Mux(hit, Mux(req.valid && !hazard, s_lookup, s_idle), s_miss),
        s_miss   -> Mux(dirty, s_aw, s_ar),
        s_aw     -> Mux(master.aw.fire, s_w, s_aw),
        s_w      -> Mux(slave.b.fire, Mux(cnt === 3.U, s_ar, s_aw), s_w),
        s_ar     -> Mux(master.ar.fire, s_r, s_ar),
        s_r      -> Mux(slave.r.fire, Mux(cnt === 3.U, s_idle, s_ar), s_r)
    ))

    val req_reg = RegEnable(req, (state === s_idle || (state === s_lookup && hit)) && req.valid && !hazard)
    val way_sel = RegEnable(random_bit(log2Ceil(way) - 1, 0), state === s_lookup)

    val hit_way = Seq.fill(way)(dontTouch(Wire(Bool())))
    val cache_line = Seq.fill(way)(dontTouch(Wire(Vec(4, UInt(64.W)))))
    val cache_line_reg = dontTouch(Reg(Vec(4, UInt(64.W))))

    cpu_slave.r.bits.data := 0.U(64.W)
    dirty := false.B
    hazard := false.B
    
    for (i <- 0 until way)
    {
        // state === s_r will write multiple times, should not matter
        // may be state === s_miss will solve this problem?
        ways(i).tag.io.cen  := ((state === s_idle || state === s_lookup) && req.valid && !hazard) || (state === s_r && way_sel === i.U)
        ways(i).tag.io.wen  := state === s_r && way_sel === i.U
        ways(i).tag.io.bwen := Fill(51, 1.U(1.W))
        ways(i).tag.io.A    := req.index
        ways(i).tag.io.D    := req_reg.tag

        ways(i).V.io.cen   := ((state === s_idle || state === s_lookup) && req.valid && !hazard) || (state === s_r && way_sel === i.U)
        ways(i).V.io.wen   := state === s_r && way_sel === i.U
        ways(i).V.io.bwen  := 1.U(1.W)
        ways(i).V.io.A     := req.index
        ways(i).V.io.D     := 1.U(1.W)

        ways(i).D.io.cen   := (state === s_lookup) || (state === s_r && way_sel === i.U)
        ways(i).D.io.wen   := (state === s_lookup && hit_way(i) && req_reg.op) || (state === s_r && way_sel === i.U)
        ways(i).D.io.bwen  := 1.U(1.W)
        ways(i).D.io.A     := req.index
        ways(i).D.io.D     := Mux(state === s_lookup && hit_way(i) && req_reg.op, 1.U(1.W), 0.U(1.W))

        for (j <- 0 until 4)
        {
            val bwen = Wire(Vec(8, UInt(8.W)))
            for (k <- 0 until 8) {
                bwen(k) := Fill(8, req_reg.strb)
            }

            ways(i).data.banks(j).cen  := ((state === s_idle || state === s_lookup) && req.valid && req.offset === j.U) || (state === s_r && way_sel === i.U && req_reg.offset === j.U)
            ways(i).data.banks(j).wen  := (state === s_lookup && hit_way(i) && req_reg.op && req_reg.offset === j.U) || (state === s_r && way_sel === i.U && req_reg.offset === j.U)
            ways(i).data.banks(j).bwen := Mux(state === s_lookup, bwen.asUInt(), Fill(63, 1.U(1.W)))
            ways(i).data.banks(j).A    := Mux(state === s_lookup && hit_way(i) && req_reg.op && req_reg.offset === j.U, req_reg.index, req.index)
            ways(i).data.banks(j).D    := req_reg.data
            cache_line(i)(j) := ways(i).data.banks(j).Q
            when (state === s_lookup && hit_way(i)) {
                cache_line_reg(j) := ways(i).data.banks(j).Q
            }

            when (state === s_lookup && hit_way(i) && req_reg.op && req_reg.offset === j.U && req_reg.offset === req.offset) {
                hazard := true.B
            }
        }

        hit_way(i) := ways(i).V.io.Q === 1.U && ways(i).tag.io.Q === req_reg.tag

        when (ways(i).D.io.Q === 1.U && way_sel === i.U) {
            dirty := true.B
        }

        when (state === s_lookup && hit_way(i)) {
            cpu_slave.r.bits.data := cache_line(i)(req_reg.offset)
        }
    }

    hit := hit_way.reduce(_ || _)

    val cache_line_buf = Reg(UInt(192.W))

    val cache_ready = dontTouch((state === s_idle || (state === s_lookup && hit)) && req.valid && !hazard)
    
    cpu_master.aw.ready := cache_ready
    cpu_master.w.ready  := cache_ready
    cpu_master.ar.ready := cache_ready

    cpu_slave.b.valid := (state === s_lookup && hit) || (state === s_r && slave.r.fire && cnt === 3.U) && req_reg.op
    cpu_slave.b.bits.resp := 0.U(2.W)

    cpu_slave.r.valid := (state === s_lookup && hit) || (state === s_r && slave.r.fire && cnt === 3.U) && !req_reg.op
    cpu_slave.r.bits.resp := 0.U(2.W)
    when (state === s_r) {
        cpu_slave.r.bits.data := Cat(slave.r.bits.data, cache_line_buf) >> Cat(req_reg.offset, 0.U(3.W))
    }

    when (cpu_master.aw.fire || cpu_master.ar.fire) {
        cnt := 0.U
    } .elsewhen (slave.b.fire || slave.r.fire) {
        cnt := cnt + 1.U
    }

    master.aw.valid := state === s_aw
    master.aw.bits.addr := Cat(req_reg.tag, req_reg.index, cnt, 0.U(3.W))
    master.aw.bits.prot := 0.U(3.W)

    master.w.valid := state === s_w
    master.w.bits.data := cache_line_reg(cnt)
    master.w.bits.strb := Fill(8, 1.U)

    master.ar.valid := state === s_ar
    master.ar.bits.addr := Cat(req_reg.tag, req_reg.index, cnt, 0.U(3.W))
    master.ar.bits.prot := 0.U(3.W)

    slave.b.ready := state === s_w

    slave.r.ready := state === s_r
    when (slave.r.fire) {
        cache_line_buf := cache_line_buf >> 64.U | Cat(slave.r.bits.data, 0.U(128.W))
    }
}