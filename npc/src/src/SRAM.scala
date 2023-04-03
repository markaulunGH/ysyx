import chisel3._
import chisel3.util._

class WaddrChannel extends Bundle
{
    val awaddr = Output(UInt(64.W))
    val awprot = Output(UInt(3.W))
}

class WdataChannel extends Bundle
{
    val wdata = Output(UInt(64.W))
    val wstrb = Output(UInt(8.W))
}

class WrespChannel extends Bundle
{
    val bresp = Output(UInt(2.W))
}

class RaddrChannel extends Bundle
{
    val araddr = Output(UInt(64.W))
    val arprot = Output(UInt(3.W))
}

class RdataChannel extends Bundle
{
    val rdata = Output(UInt(64.W))
    val rresp = Output(UInt(2.W))
}

class AXI_Lite extends Bundle
{
    val waddr = Decoupled(new WaddrChannel)
    val wdata = Decoupled(new WdataChannel)
    val wresp = Filpped(Decoupled(new WrespChannel))
    val raddr = Decoupled(new RaddrChannel)
    val rdata = Flipped(Decoupled(new RdataChannel))
}

class SRAM extends Module
{
    val io = IO(new Bundle
    {
        val axi = Flipped(new AXI_Lite)
    })

    val rdata = RegInit(0.U(64.W))
}