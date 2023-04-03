import chisel3._
import chisel3.util._

class AXI_Lite extends Bundle
{
    val awvalid = Output(Bool())
    val awready = Input(Bool())
    val awaddr  = Output(UInt(64.W))
    val awprot  = Output(UInt(3.W))

    val wvalid  = Output(Bool())
    val wready  = Input(Bool())
    val wdata   = Output(UInt(64.W))
    val wstrb   = Output(UInt(8.W))

    val bvalid  = Input(Bool())
    val bready  = Output(Bool())
    val bresp   = Input(UInt(2.W))

    val arvalid = Output(Bool())
    val arready = Input(Bool())
    val araddr  = Output(UInt(64.W))
    val arprot  = Output(UInt(3.W))

    val rvalid  = Input(Bool())
    val rready  = Output(Bool())
    val rdata   = Input(UInt(64.W))
    val rresp   = Input(UInt(2.W))
}

class SRAM extends Module
{
    val io = IO(new Bundle
    {
        val axi = Flipped(new AXI_Lite)
    })

    
}