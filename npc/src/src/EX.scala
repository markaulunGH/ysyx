import chisel3._
import chisel3.util._

class EX extends Module
{
    val io = IO(new Bundle
    {
        val ID_EX = Flipped(new ID_EX)
        val EX_MM = new EX_MM
    })
    val alu = Module(new alu)
    io.ID_EX.alu <> alu.io.in

    io.EX_MM.alu_result := alu.io.alu_result
    io.EX_MM.wen := io.ID_EX.wen
    io.EX_MM.waddr := io.ID_EX.waddr
}