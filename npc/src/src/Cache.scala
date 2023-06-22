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

class Bank_IO(depth : Int, bank : Int) extends Bundle
{
    val Q    = Output(UInt(64.W))
    val cen  = Input(Bool())
    val wen  = Input(Bool())
    val bwen = Input(UInt(64.W))
    val A    = Input(UInt(log2Ceil(depth).W))
    val D    = Input(UInt(64.W))
}

class Cache_Line(depth : Int, bank : Int) extends Module
{
    val banks = Seq.fill(bank)(IO(new Bank_IO(depth, bank)))
    val datas = Seq.fill(bank)(Module(new Cache_Sram(64, depth)))

    for (i <- 0 until bank) {
        datas(i).io <> banks(i)
    }
}

class Cache_Way(depth : Int, bank : Int) extends Bundle
{
    val tag  = Module(new Cache_Sram(64 - log2Ceil(depth) - (log2Ceil(bank) + 3), depth))
    val V    = Module(new Cache_Sram(1, depth))
    val D    = Module(new Cache_Sram(1, depth))
    val data = Module(new Cache_Line(depth, bank))
}

class Cache_Req(depth : Int, bank : Int) extends Bundle
{
    val op     = Bool()
    val tag    = UInt((64 - log2Ceil(depth) - (log2Ceil(bank) + 3)).W)
    val index  = UInt(log2Ceil(depth).W)
    val offset = UInt((log2Ceil(bank) + 3).W)
    val data   = UInt(64.W)
    val strb   = UInt(8.W)
    val device = Bool()
}

class Cache(way : Int, depth : Int, bank : Int) extends Module
{
    val index_len = log2Ceil(depth)
    val offset_len = log2Ceil(bank) + 3
    val tag_len = 64 - index_len - offset_len

    val cpu_master = IO(Flipped(new AXI_Lite_Master))
    val cpu_slave  = IO(Flipped(new AXI_Lite_Slave))
    val master     = IO(new AXI_Lite_Master)
    val slave      = IO(new AXI_Lite_Slave)

    val ways = Seq.fill(way)(new Cache_Way(depth, bank))
    val random_bit = LFSR(16)

    // we require that cpu always send w and aw request in one cycle
    val cpu_request = cpu_master.ar.valid || (cpu_master.aw.valid && cpu_master.w.valid)
    val cpu_ready = cpu_slave.r.ready || cpu_slave.b.ready

    val req = dontTouch(Wire(new Cache_Req(depth, bank)))
    req.op     := cpu_master.aw.valid
    req.tag    := Mux(req.op, cpu_master.aw.bits.addr(63, index_len + offset_len), cpu_master.ar.bits.addr(63, index_len + offset_len))
    req.index  := Mux(req.op, cpu_master.aw.bits.addr(index_len + offset_len - 1, offset_len), cpu_master.ar.bits.addr(index_len + offset_len - 1, offset_len))
    req.offset := Mux(req.op, cpu_master.aw.bits.addr(offset_len - 1, 0), cpu_master.ar.bits.addr(offset_len - 1, 0))
    req.data   := cpu_master.w.bits.data
    req.strb   := cpu_master.w.bits.strb
    req.device := cpu_master.ar.bits.addr(31, 28) === 0xa.U || cpu_master.aw.bits.addr(31, 28) === 0xa.U

    val dirty = dontTouch(Wire(Bool()))
    val hazard = dontTouch(Wire(Bool()))
    val hit = dontTouch(Wire(Bool()))
    val cnt = RegInit(0.U(log2Ceil(bank).W))
    val awfire = RegInit(false.B)
    val wfire = RegInit(false.B)

    val s_idle :: s_lookup :: s_miss :: s_aw :: s_b :: s_ar :: s_r :: s_wait :: s_bypass :: Nil = Enum(9)
    val state = RegInit(s_idle)

    state := MuxLookup(state, s_idle, Seq(
        s_idle   -> Mux(cpu_request, Mux(req.device, s_bypass, s_lookup), s_idle),
        s_lookup -> Mux(hit, Mux(cpu_ready, Mux(cpu_request, Mux(req.device, s_bypass, Mux(hazard, s_idle, s_lookup)), s_idle), s_wait), s_miss),
        s_wait   -> Mux(cpu_ready, s_idle, s_wait),
        s_miss   -> Mux(dirty, s_aw, s_ar),
        s_aw     -> Mux((master.aw.fire || awfire) && (master.w.fire || wfire), s_b, s_aw),
        s_b      -> Mux(slave.b.fire, Mux(cnt === (bank - 1).U, s_ar, s_aw), s_b),
        s_ar     -> Mux(master.ar.fire, s_r, s_ar),
        s_r      -> Mux(slave.r.fire, Mux(cnt === (bank - 1).U, Mux(cpu_ready, s_idle, s_wait), s_ar), s_r),
        s_bypass -> Mux(slave.b.fire || slave.r.fire, s_idle, s_bypass)
    ))

    val cache_ready = (state === s_idle || (state === s_lookup && hit)) && !hazard

    val req_reg = RegEnable(req, cache_ready && cpu_request)
    val bwen = Wire(Vec(8, UInt(8.W)))
    for (i <- 0 until 8) {
        bwen(i) := Fill(8, req_reg.strb(i))
    }

    val way_sel = Seq.fill(way)(dontTouch(Wire(Bool())))
    val way_sel_reg = Seq.fill(way)(Reg(Bool()))
    for (i <- 0 until way) {
        way_sel(i) := random_bit(log2Ceil(way) - 1, 0) === i.U
        when (state === s_lookup) {
            way_sel_reg(i) := way_sel(i)
        }
    }
    
    val cache_line = Reg(Vec(bank, UInt(64.W)))
    val cache_line_tag = Reg(UInt(tag_len.W))

    val cache_line_buf = Reg(UInt((64 * (bank - 1)).W))
    val new_cache_line = dontTouch(Cat(slave.r.bits.data, cache_line_buf))
    
    val cache_rdata = dontTouch(Wire(UInt(64.W)))
    val cache_rdata_reg = RegEnable(cache_rdata, state === s_lookup || state === s_r)
    
    val refill_wen = state === s_r && cnt === 3.U
    
    val hit_way = Seq.fill(way)(dontTouch(Wire(Bool())))

    dirty := false.B
    hazard := false.B
    cache_rdata := 0.U(64.W)

    for (i <- 0 until way)
    {
        hit_way(i) := ways(i).V.io.Q === 1.U && ways(i).tag.io.Q === req_reg.tag

        when (state === s_lookup && way_sel(i)) {
            cache_line_tag := ways(i).tag.io.Q
        }

        when (ways(i).D.io.Q === 1.U && way_sel_reg(i)) {
            dirty := true.B
        }

        ways(i).tag.io.cen  := (cache_ready && cpu_request) || (refill_wen && way_sel_reg(i))
        ways(i).tag.io.wen  := refill_wen && way_sel_reg(i)
        ways(i).tag.io.A    := Mux(state === s_r, req_reg.index, req.index)
        ways(i).tag.io.bwen := Fill(tag_len, 1.U(1.W))
        ways(i).tag.io.D    := req_reg.tag

        ways(i).V.io.cen   := (cache_ready && cpu_request) || (refill_wen && way_sel_reg(i))
        ways(i).V.io.wen   := refill_wen && way_sel_reg(i)
        ways(i).V.io.A     := Mux(state === s_r, req_reg.index, req.index)
        ways(i).V.io.bwen  := 1.U(1.W)
        ways(i).V.io.D     := 1.U(1.W)

        ways(i).D.io.cen   := (state === s_lookup) || (refill_wen && way_sel_reg(i))
        ways(i).D.io.wen   := (state === s_lookup && hit_way(i) && req_reg.op) || (refill_wen && way_sel_reg(i))
        ways(i).D.io.A     := req_reg.index
        ways(i).D.io.bwen  := 1.U(1.W)
        ways(i).D.io.D     := Mux(refill_wen && way_sel_reg(i) && !req_reg.op, 0.U(1.W), 1.U(1.W))

        val hit_bank = Seq.fill(bank)(Wire(Bool()))

        for (j <- 0 until bank)
        {
            hit_bank(j) := req_reg.offset(offset_len - 1, 3) === j.U

            when (state === s_lookup && hit_way(i) && hit_bank(j) && req_reg.op && req_reg.offset(offset_len - 1, 3) === req.offset(offset_len - 1, 3)) {
                hazard := true.B
            }

            when (state === s_lookup && hit_way(i) && hit_bank(j)) {
                cache_rdata := ways(i).data.banks(j).Q >> Cat(req_reg.offset(2, 0), 0.U(3.W))
            }

            when (state === s_lookup && way_sel(i)) {
                cache_line(j) := ways(i).data.banks(j).Q
            }

            ways(i).data.banks(j).cen  := (cache_ready && cpu_request) || (state === s_lookup) || (refill_wen && way_sel_reg(i))
            ways(i).data.banks(j).wen  := (state === s_lookup && hit_way(i) && hit_bank(j) && req_reg.op) || (refill_wen && way_sel_reg(i))
            ways(i).data.banks(j).A    := Mux(state === s_r || state === s_lookup && hit_way(i) && hit_bank(j) && req_reg.op, req_reg.index, req.index)
            ways(i).data.banks(j).bwen := Mux(state === s_r, Fill(64, 1.U(1.W)), bwen.asUInt() << Cat(req_reg.offset(2, 0), 0.U(3.W)))
            ways(i).data.banks(j).D    := Mux(state === s_r, Mux(hit_bank(j) && req_reg.op,
                ((req_reg.data << Cat(req_reg.offset(2, 0), 0.U(3.W)) & (bwen.asUInt() << Cat(req_reg.offset(2, 0), 0.U(3.W))))) |
                (new_cache_line >> Cat(j.U, 0.U(6.W)) & ~(bwen.asUInt() << Cat(req_reg.offset(2, 0), 0.U(3.W)))),
                new_cache_line >> Cat(j.U, 0.U(6.W))),
                req_reg.data << Cat(req_reg.offset(2, 0), 0.U(3.W)))
        }
    }

    hit := hit_way.reduce(_ || _)

    when (state === s_r) {
        cache_rdata := new_cache_line >> Cat(req_reg.offset, 0.U(3.W))
    }

    val bypass = state === s_bypass

    cpu_master.aw.ready := cache_ready
    cpu_master.w.ready  := cache_ready
    cpu_master.ar.ready := cache_ready

    cpu_slave.b.valid     := Mux(bypass, slave.b.valid, ((state === s_lookup && hit) || (state === s_r && slave.r.fire && cnt === (bank - 1).U) || state === s_wait) && req_reg.op)
    cpu_slave.b.bits.resp := Mux(bypass, slave.b.bits.resp, 0.U(2.W))

    cpu_slave.r.valid     := Mux(bypass, slave.r.valid, ((state === s_lookup && hit) || (state === s_r && slave.r.fire && cnt === (bank - 1).U) || state === s_wait) && !req_reg.op)
    cpu_slave.r.bits.resp := Mux(bypass, slave.r.bits.resp, 0.U(2.W))
    cpu_slave.r.bits.data := Mux(bypass, slave.r.bits.data, Mux(state === s_wait, cache_rdata_reg, cache_rdata))

    when (cpu_master.aw.fire || cpu_master.ar.fire) {
        cnt := 0.U(log2Ceil(bank).W)
    } .elsewhen (slave.b.fire || slave.r.fire) {
        cnt := cnt + 1.U(log2Ceil(bank).W)
    }

    master.aw.valid     := Mux(bypass, req_reg.op, state === s_aw) && !awfire
    master.aw.bits.addr := Mux(bypass, Cat(req_reg.tag, req_reg.index, req_reg.offset), Cat(cache_line_tag, req_reg.index, cnt, 0.U(3.W)))
    master.aw.bits.prot := 0.U(3.W)
    when (slave.b.fire) {
        awfire := false.B
    } .elsewhen (master.aw.fire) {
        awfire := true.B
    }

    master.w.valid     := Mux(bypass, req_reg.op, state === s_aw) && !wfire
    master.w.bits.data := Mux(bypass, req_reg.data, cache_line(cnt))
    master.w.bits.strb := Mux(bypass, req_reg.strb, Fill(8, 1.U))
    when (slave.b.fire) {
        wfire := false.B
    } .elsewhen (master.w.fire) {
        wfire := true.B
    }

    master.ar.valid     := Mux(bypass, !req_reg.op, state === s_ar)
    master.ar.bits.addr := Mux(bypass, Cat(req_reg.tag, req_reg.index, req_reg.offset), Cat(req_reg.tag, req_reg.index, cnt, 0.U(3.W)))
    master.ar.bits.prot := 0.U(3.W)

    slave.b.ready := Mux(bypass, cpu_slave.b.ready, state === s_b)

    slave.r.ready := Mux(bypass, cpu_slave.r.ready, state === s_r)
    when (slave.r.fire) {
        cache_line_buf := cache_line_buf >> 64.U | Cat(slave.r.bits.data, 0.U((64 * (bank - 2)).W))
    }
}