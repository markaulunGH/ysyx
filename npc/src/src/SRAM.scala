import chisel3._
import chisel3.util._

class SRAM extends Module
{
    val io = IO(Flipped(new AXI_Lite_IO))
}