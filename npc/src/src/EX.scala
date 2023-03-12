import chisel3._
import chisel3.util._

class EX extends Module
{
    val io = IO(new Bundle
    {
        val ID_EX = Flipped(new ID_EX)
        val EX_MM = Flipped(new EX_MM)
        val alu_io = new alu_io
    })

    io.alu_io.alu_op := io.ID_EX.alu_op
    io.alu_io.alu_src1 := io.ID_EX.alu_src1
    io.alu_io.alu_src2 := io.ID_EX.alu_src2
    
    val alu = Module(new alu)
    io.alu_io <> alu.io
}