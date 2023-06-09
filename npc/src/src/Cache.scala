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

    val s_idle :: s_lookup :: s_aw :: s_w :: s_ar :: s_r :: Nil = Enum(5)
    val state = RegInit(s_idle)

    val cache_req = cpu_master.ar.valid || cpu_master.aw.valid
    val hazard = Wire(Bool())
    val cache_hit = Wire(Bool())
    val write_finish = Wire(Bool())
    val read_finish = Wire(Bool())
    val cnt = RegInit(0.U(2.W))

    state := MuxLookup(state, s_idle, Seq(
        s_idle -> Mux(cache_req && !hazard, s_lookup, s_idle),
        s_lookup -> Mux(cache_hit, Mux(cache_req && !hazard, s_lookup, s_idle), s_aw),
        // what if cacheline is invalid?
    ))

    val cpu_master_reg = RegEnable(cpu_master, state === s_idle)

    val way0 = new Way
    val way1 = new Way
}