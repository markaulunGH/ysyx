import chisel3._
import chisel3.util._

class AXI extends Module
{
    val io = IO(new AXI_Lite)

    val arinit :: araddr :: Nil = Enum(2)
    val arstate = RegInit(arinit)
    switch (arstate)
    {
        is (arinit) { arstate := Mux(io.ar.valid, araddr, arinit) }
        is (araddr) { arstate := Mux(io.ar.ready, arinit, araddr) }
    }

    val rinit :: rdata :: Nil = Enum(2)
    val rstate = RegInit(rinit)
    switch (rstate)
    {
        is (rinit) { rstate := Mux(io.r.valid, rdata, rinit) }
        is (rdata) { rstate := rinit }
    }

    val winit :: waddr :: wdata :: Nil = Enum(3)
    val wstate = RegInit(winit)
    switch (wstate)
    {
        is (winit) { wstate := Mux(io.aw.valid, waddr, winit) }
        is (waddr) { wstate := Mux(io.aw.ready, wdata, waddr) }
        is (wdata) { wstate := Mux(io.w.ready, winit, wdata) }
    }
}