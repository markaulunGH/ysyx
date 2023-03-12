import chisel3._
import chisel3.util._

class alu_io extends Bundle
{
    val alu_op = Input(Vec(19, UInt(1.W)))
    val alu_src1 = Input(UInt(64.W))
    val alu_src2 = Input(UInt(64.W))
    val alu_result = Output(UInt(64.W))
}

class alu extends Module
{
    val io = IO(new Bundle
    { val alu_io = new alu_io})

    val op_add = io.alu_io.alu_op(0)

    when (op_add === 1.U)
    {
        io.alu_io.alu_result := io.alu_io.alu_src1 + io.alu_io.alu_src2
    }
    .otherwise
    {
        io.alu_io.alu_result := 0.U
    }
}