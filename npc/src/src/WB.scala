import chisel3._
import chisel3.util._

class WB extends Module
{
    val io = IO(new Bundle
    {
        val MM_WB = Flipped(new MM_WB)

        val reg_w = Flipped(new reg_w)
    })

    io.reg_w.wen := io.MM_WB.wen
    io.reg_w.waddr := io.MM_WB.waddr
    io.reg_w.wdata := io.MM_WB.alu_result
}