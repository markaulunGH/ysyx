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
    when (io.cen && io.wen) {
        ram(io.A) := (io.D & io.bwen) | (ram(io.A) & ~io.bwen)
    }
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

    val cpu_request = dontTouch(cpu_master.ar.valid || cpu_master.aw.valid)
    val cpu_ready = dontTouch(cpu_slave.r.ready || cpu_slave.b.ready)

    val req = dontTouch(Wire(new Cache_Req))
    req.op     := cpu_master.aw.valid
    req.tag    := Mux(req.op, cpu_master.aw.bits.addr(63, 11), cpu_master.ar.bits.addr(63, 11))
    req.index  := Mux(req.op, cpu_master.aw.bits.addr(10, 5), cpu_master.ar.bits.addr(10, 5))
    req.offset := Mux(req.op, cpu_master.aw.bits.addr(4, 0), cpu_master.ar.bits.addr(4, 0))
    req.data   := cpu_master.w.bits.data
    req.strb   := cpu_master.w.bits.strb

    val dirty = dontTouch(Wire(Bool()))
    val hazard = dontTouch(Wire(Bool()))
    val hit = dontTouch(Wire(Bool()))
    val cnt = RegInit(0.U(2.W))

    val s_idle :: s_lookup :: s_miss :: s_aw :: s_w :: s_ar :: s_r :: s_wait :: Nil = Enum(8)
    val state = RegInit(s_idle)

    state := MuxLookup(state, s_idle, Seq(
        s_idle   -> Mux(cpu_request && !hazard, s_lookup, s_idle),
        s_lookup -> Mux(hit, Mux(cpu_ready, Mux(cpu_request && !hazard, s_lookup, s_idle), s_wait), s_miss),
        // Now cache state will change from idle -> lookup -> wait -> idle, maybe wait -> lookup directly in the future
        s_wait   -> Mux(cpu_ready, s_idle, s_wait),
        s_miss   -> Mux(dirty, s_aw, s_ar),
        s_aw     -> Mux(master.aw.fire, s_w, s_aw),
        s_w      -> Mux(slave.b.fire, Mux(cnt === 3.U, s_ar, s_aw), s_w),
        s_ar     -> Mux(master.ar.fire, s_r, s_ar),
        s_r      -> Mux(slave.r.fire, Mux(cnt === 3.U, Mux(cpu_ready, s_idle, s_wait), s_ar), s_r)
    ))

    val cache_ready = dontTouch((state === s_idle || (state === s_lookup && hit)) && !hazard)

    val req_reg = RegEnable(req, cache_ready && cpu_request)
    val way_sel = RegEnable(random_bit(log2Ceil(way) - 1, 0), state === s_lookup)

    val hit_way = Seq.fill(way)(dontTouch(Wire(Bool())))
    val cache_line = Seq.fill(way)(dontTouch(Wire(Vec(4, UInt(64.W)))))
    val cache_line_reg = dontTouch(Reg(Vec(4, UInt(64.W))))

    val cache_line_buf = Reg(UInt(192.W))
    val new_cache_line = dontTouch(Cat(slave.r.bits.data, cache_line_buf))

    val cache_rdata = Wire(UInt(64.W))
    val cache_rdata_reg = RegEnable(cache_rdata, state === s_lookup || state === s_r)
    cache_rdata := 0.U(64.W)
    dirty := false.B
    hazard := false.B

    val write_back_en = dontTouch(state === s_r)
    
    for (i <- 0 until way)
    {
        // state === s_r will write multiple times, should not matter
        // may be state === s_miss will solve this problem?
        ways(i).tag.io.cen  := (cache_ready && cpu_request) || (write_back_en && way_sel === i.U)
        ways(i).tag.io.wen  := write_back_en && way_sel === i.U
        ways(i).tag.io.bwen := Fill(53, 1.U(1.W))
        ways(i).tag.io.A    := Mux(state === s_r, req_reg.index, req.index)
        ways(i).tag.io.D    := req_reg.tag

        ways(i).V.io.cen   := (cache_ready && cpu_request) || (write_back_en && way_sel === i.U)
        ways(i).V.io.wen   := write_back_en && way_sel === i.U
        ways(i).V.io.bwen  := 1.U(1.W)
        ways(i).V.io.A     := Mux(state === s_r, req_reg.index, req.index)
        ways(i).V.io.D     := 1.U(1.W)

        ways(i).D.io.cen   := (state === s_lookup) || (write_back_en && way_sel === i.U)
        ways(i).D.io.wen   := (state === s_lookup && hit_way(i) && req_reg.op) || (write_back_en && way_sel === i.U)
        ways(i).D.io.bwen  := 1.U(1.W)
        ways(i).D.io.A     := Mux(state === s_r, req_reg.index, req.index)
        ways(i).D.io.D     := Mux(state === s_lookup && hit_way(i) && req_reg.op, 1.U(1.W), 0.U(1.W))

        for (j <- 0 until 4)
        {
            val bwen = Wire(Vec(8, UInt(8.W)))
            for (k <- 0 until 8) {
                bwen(k) := Fill(8, req_reg.strb)
            }

            ways(i).data.banks(j).cen  := (cache_ready && cpu_request && req.offset(4, 3) === j.U) || (write_back_en && way_sel === i.U)
            ways(i).data.banks(j).wen  := (state === s_lookup && hit_way(i) && req_reg.op && req_reg.offset(4, 3) === j.U) || (write_back_en && way_sel === i.U)
            ways(i).data.banks(j).bwen := Mux(state === s_r, Fill(64, 1.U(1.W)), bwen.asUInt())
            ways(i).data.banks(j).A    := Mux(state === s_r || state === s_lookup && hit_way(i) && req_reg.op && req_reg.offset(4, 3) === j.U, req_reg.index, req.index)
            ways(i).data.banks(j).D    := Mux(state === s_r, new_cache_line >> Cat(j.U, 0.U(6.W)), req_reg.data)
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
            for (j <- 0 until 4) {
                when (req_reg.offset(4, 3) === j.U) {
                    cache_rdata := cache_line(i)(j) >> Cat(req_reg.offset(2, 0), 0.U(3.W))
                }
            }
        }
    }

    hit := hit_way.reduce(_ || _)

    cpu_master.aw.ready := cache_ready
    cpu_master.w.ready  := cache_ready
    cpu_master.ar.ready := cache_ready

    cpu_slave.b.valid := (state === s_lookup && hit) || (state === s_r && slave.r.fire && cnt === 3.U) && req_reg.op || state === s_wait
    cpu_slave.b.bits.resp := 0.U(2.W)

    cpu_slave.r.valid := (state === s_lookup && hit) || (state === s_r && slave.r.fire && cnt === 3.U) && !req_reg.op || state === s_wait
    cpu_slave.r.bits.resp := 0.U(2.W)
    when (state === s_r) {
        cache_rdata := new_cache_line >> Cat(req_reg.offset, 0.U(3.W))
    }
    cpu_slave.r.bits.data := Mux(state === s_wait, cache_rdata_reg, cache_rdata)

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