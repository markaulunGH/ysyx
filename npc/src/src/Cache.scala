import chisel3._
import chisel3.util._
import chisel3.util.random._

class Cache_Sram(width : Int, depth : Int) extends Module
{
    val io = IO(new Bundle
    {
        val wen  = Input(Bool())
        val addr = Input(UInt(log2Ceil(depth).W))
        val din  = Output(UInt(width.W))
        val dout = Input(UInt(width.W))
    })

    val rf = RegInit(Vec(depth, 0.U(width.W)))
    rf(io.addr) := io.dout
    io.din := rf(io.addr)
}

class Way extends Bundle
{
    val tagV = new Cache_Sram(52, 256)
    val dirty = new Cache_Sram(1, 256)
    val data0 = new Cache_Sram(64, 256)
    val data1 = new Cache_Sram(64, 256)
    val data2 = new Cache_Sram(64, 256)
    val data3 = new Cache_Sram(64, 256)
}

class Cache extends Module
{
    val cpu_master = IO(Flipped(new AXI_Lite_Master))
    val cpu_slave  = IO(Flipped(new AXI_Lite_Slave))
    val master     = IO(new AXI_Lite_Master)
    val slave      = IO(new AXI_Lite_Slave)

    val req = new Bundle
    {
        val valid  = cpu_master.ar.valid || cpu_master.aw.valid
        val op     = cpu_master.aw.valid
        val tag    = Mux(op, cpu_master.aw.bits.addr(63, 13), cpu_master.ar.bits.addr(63, 13))
        val index  = Mux(op, cpu_master.aw.bits.addr(12, 5), cpu_master.ar.bits.addr(12, 5))
        val offset = Mux(op, cpu_master.aw.bits.addr(4, 0), cpu_master.ar.bits.addr(4, 0))
    }

    val random_bit = LFSR(1)

    val s_idle :: s_lookup :: s_aw :: s_w :: s_ar :: s_r :: Nil = Enum(5)
    val state = RegInit(s_idle)

    val hazard = Wire(Bool())
    val hit = Wire(Bool())
    val write_finish = Wire(Bool())
    val read_finish = Wire(Bool())
    val cnt = RegInit(0.U(2.W))

    state := MuxLookup(state, s_idle, Seq(
        s_idle -> Mux(req.valid && !hazard, s_lookup, s_idle),
        s_lookup -> Mux(hit, Mux(req.valid && !hazard, s_lookup, s_idle), s_aw),
        s_aw -> Mux(master.aw.fire, s_w, s_aw),
        s_w -> Mux(slave.b.fire, Mux(cnt === 2.U, s_ar, s_aw), s_w),
        s_ar -> Mux(master.ar.fire, s_r, s_ar),
        s_r -> Mux(slave.r.fire, Mux(cnt === 2.U, s_idle, s_r), s_r)
    ))

    val req_reg = RegEnable(req, state === s_idle)
    val way_sel_reg = RegEnable(random_bit, state === s_lookup)
    val state_reg = RegNext(state)

    val way0 = new Way
    val way1 = new Way
    val way = Mux(way_sel_reg.asBool(), way1, way0)

    val hit_way0 = way0.tagV.io.dout(0) && way0.tagV.io.dout(51, 1) === req_reg.tag
    val hit_way1 = way1.tagV.io.dout(0) && way1.tagV.io.dout(51, 1) === req_reg.tag
    hit := hit_way0 || hit_way1

    // master.aw.bits.addr = Cat()
}