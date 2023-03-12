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
}