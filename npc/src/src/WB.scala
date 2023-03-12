import chisel3._
import chisel3.util._

class WB extends Module
{
    val io = IO(new Bundle
    {
        val MM_WB = Flipped(new MM_WB)
        val WB_top = new WB_top

        val reg_w = Flipped(new reg_w)
    })
}