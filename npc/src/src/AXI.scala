import chisel3._
import chisel3.util._

class WaddrChannel extends Bundle
{
    val addr = Output(UInt(64.W))
    val prot = Output(UInt(3.W))
}

class WdataChannel extends Bundle
{
    val data = Output(UInt(64.W))
    val strb = Output(UInt(8.W))
}

class WrespChannel extends Bundle
{
    val resp = Output(UInt(2.W))
}

class RaddrChannel extends Bundle
{
    val addr = Output(UInt(64.W))
    val prot = Output(UInt(3.W))
}

class RdataChannel extends Bundle
{
    val data = Output(UInt(64.W))
    val resp = Output(UInt(2.W))
}

class AXI_Lite_IO extends Bundle
{
    val aw = Decoupled(new WaddrChannel)
    val w  = Decoupled(new WdataChannel)
    val b  = Flipped(Decoupled(new WrespChannel))
    val ar = Decoupled(new RaddrChannel)
    val r  = Flipped(Decoupled(new RdataChannel))
}

class AXI extends Module
{
    val io = IO(new AXI_Lite_IO)

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