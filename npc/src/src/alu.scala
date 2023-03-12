import chisel3._
import chisel3.util._

class alu_in extends Bundle
{
    val alu_op = Input(Vec(19, UInt(1.W)))
    val alu_src1 = Input(UInt(64.W))
    val alu_src2 = Input(UInt(64.W))
}

class alu extends Module
{
    val io = IO(new Bundle
    {
        val in = new alu_in
        val alu_result = Output(UInt(64.W))
    })

    val op_add = io.in.alu_op(0)

    when (op_add === 1.U)
    {
        io.alu_result := io.in.alu_src1 + io.in.alu_src2
    }
    .otherwise
    {
        io.alu_result := 0.U
    }
}