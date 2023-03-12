import chisel3._
import chisel3.util._

class MM extends Module
{
    val io = IO(new Bundle
    {
        val EX_MM = Flipped(new EX_MM)
        val MM_WB = new MM_WB
    })

    io.MM_WB.alu_result := io.EX_MM.alu_result
    io.MM_WB.wen := io.EX_MM.wen
    io.MM_WB.waddr := io.EX_MM.waddr
}