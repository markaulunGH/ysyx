import chisel3._
import chisel3.util._

class SRAM extends Module
{
    val io = IO(new Bundle
    {
        val master = Flipped(new AXI_Lite_Master)
        val slave  = Flipped(new AXI_Lite_Slave)
    })
}