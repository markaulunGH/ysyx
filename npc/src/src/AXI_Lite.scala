import chisel3._
import chisel3.util._

class AXI_Lite extends Module
{
    val io = IO(new Bundle
    {
        val master = new AXI_Lite_Master
        val slave = new AXI_Lite_Slave
    })

    val arinit :: araddr :: Nil = Enum(2)
    val arstate = RegInit(arinit)

    arstate := MuxLookup(arstate, arinit, Seq(
        arinit -> Mux(io.master.ar.valid, araddr, arinit),
        araddr -> Mux(io.master.ar.ready, arinit, araddr)
    ))

    val rinit :: rdata :: Nil = Enum(2)
    val rstate = RegInit(rinit)
    rstate := MuxLookup(rstate, rinit, Seq(
        rinit -> Mux(io.slave.r.valid, rdata, rinit),
        rdata -> rinit
    ))

    val winit :: waddr :: wdata :: Nil = Enum(3)
    val wstate = RegInit(winit)
    wstate := MuxLookup(wstate, winit, Seq(
        winit -> Mux(io.master.aw.valid, waddr, winit),
        waddr -> Mux(io.master.aw.ready, wdata, waddr),
        wdata -> Mux(io.master.w.ready, winit, wdata)
    ))

    val binit :: bresp :: Nil = Enum(2)
    val bstate = RegInit(binit)
    bstate := MuxLookup(bstate, binit, Seq(
        binit -> Mux(io.slave.b.valid, bresp, binit)
        bresp -> binit
    ))
}