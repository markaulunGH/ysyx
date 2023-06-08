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

class Cache(way : Int) extends Module
{
    val cpu_master = IO(Flipped(new AXI_Lite_Master))
    val cpu_slave  = IO(Flipped(new AXI_Lite_Slave))
    val master     = IO(new AXI_Lite_Master)
    val slave      = IO(new AXI_Lite_Slave)

    val s_idle :: s_lookup :: s_miss :: s_replace :: s_refill :: Nil = Enum(5)
    val state = RegInit(s_idle)

    state := MuxLookup(state, s_idle, Seq(
        s_idle -> Mux(valid || !hazard, s_lookup, s_idle),
        s_lookup -> Mux(cache_hit, Mux(valid && !hazard, s_lookup, s_idle), s_miss),
        s_miss -> Mux(master.aw.ready, s_replace, s_miss),
        s_replace -> Mux(master.ar.ready, s_refill, s_replace),
        s_refill -> Mux(master.w.ready, s_idle, s_refill)
    ))

    val s_wbidle :: s_wbwrite :: Nil = Enum(2)
    val wb_state = RegInit(s_wbidle)

    wb_state := MuxLookup(wb_state, s_wbidle, Seq(
        s_wbidle -> Mux(state === s_lookup && cache_hit ?, s_wbwrite, s_wbidle),
        s_wbwrite -> Mux(state === s_lookup && cache_hit ?, s_wbwrite, s_idle)
    ))

    val pseudoRandomNumber = LFSR(way - 1)

    for (i <- 0 to way)
    {
        
    }
}