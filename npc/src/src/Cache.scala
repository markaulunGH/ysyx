import chisel3._
import chisel3.util._
import chisel3.util.random._

class Cache_Sram(n : UInt, width : UInt) extends Module
{
    val io = new Bundle
    {
        val addr = Input(UInt(log2Ceil(n)))
        val rdata = Output(UInt(width))
        val wen = Input(Bool())
        val wdata = Input(UInt(width))
    }

    val rf[256] = RegInit(VecInit(Seq.fill(256)(0.U(1.W))))
    rf[io.addr] := io.wdata
    io.rdata := rf[io.addr]
}

class Cache_

class Cache extends Module
{
    val cpu_master = IO(Flipped(new AXI_Lite_Master))
    val cpu_slave  = IO(Flipped(new AXI_Lite_Slave))
    val master     = IO(new AXI_Lite_Master)
    val slave      = IO(new AXI_Lite_Slave)

    val pseudoRandomNumber = LFSR(1)

    val s_idle :: s_lookup :: s_miss :: s_replace :: s_refill :: Nil = Enum(5)
    val state = MuxLookup(state, s_idle, Seq(
        s_idle -> Mux(valid || !hazard, s_lookup, s_idle),
        s_lookup -> Mux(cache_hit, Mux(valid && !hazard, s_lookup, s_idle), s_miss),
        s_miss -> Mux(master.aw.ready, s_replace, s_miss),
        s_replace -> Mux(master.ar.ready, s_refill, s_replace),
        s_refill -> Mux(master.w.ready, s_idle, s_refill)
    ))

    val cache_reg = RegEnable()
    
    val s_wbidle :: s_wbwrite :: Nil = Enum(2)
    val wb_state = MuxLookup(wb_state, s_wbidle, Seq(
        s_wbidle -> Mux(state === s_lookup && cache_hit ?, s_wbwrite, s_wbidle),
        s_wbwrite -> Mux(state === s_lookup && cache_hit ?, s_wbwrite, s_idle)
    ))

}