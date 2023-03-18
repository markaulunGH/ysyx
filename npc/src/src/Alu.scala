import chisel3._
import chisel3.util._

class Alu_in extends Bundle
{
    val alu_op = Input(Vec(18, Bool()))
    val alu_src1 = Input(UInt(64.W))
    val alu_src2 = Input(UInt(64.W))
}

class Alu extends Module
{
    val io = IO(new Bundle
    {
        val in = new Alu_in
        val alu_result = Output(UInt(64.W))
    })

    val alu_op = io.in.alu_op
    val src1 = io.in.alu_src1
    val src2 = io.in.alu_src2

    val adder_a = src1
    val adder_b = Mux(alu_op(1) || alu_op(2) || alu_op(3), ~src2, src2)
    val adder_cin = Mux(alu_op(1) || alu_op(2) || alu_op(3), 1.U(1.W), 0.U(1.W))
    val adder_cout_result = adder_a + adder_b + adder_cin
    val adder_result = adder_cout_result(63, 0)
    val adder_cout = adder_cout_result(64)

    io.alu_result := MuxCase (
        0.U(64.W),
        Seq(
            alu_op(0)  -> adder_result,
            alu_op(1)  -> adder_result,
            alu_op(2)  -> Cat(0.U(63.W), (src1(63) & ~src2(63)) | (~(src1(63) ^ src2(31)) & adder_result(63))),
            alu_op(3)  -> Cat(0.U(63.W), ~adder_cout),
            alu_op(4)  -> (src1 ^ src2),
            alu_op(5)  -> (src1 | src2),
            alu_op(6)  -> (src1 & src2),
            alu_op(7)  -> (src1 << src2),
            alu_op(8)  -> (src1 >> src2),
            alu_op(9)  -> (Cat(Fill(src1(63), src1)) >> src2),
            alu_op(10) -> src1 * src2,
            alu_op(11) -> (src1.asSInt() * src2.asSInt())(63, 0),
            alu_op(12) -> (src1 * src2)(63, 0),
            alu_op(13) -> (src1.asSInt() * src2)(63, 0),
            alu_op(14) -> src1.asSInt() / src2.asSInt,
            alu_op(15) -> src1 / src2,
            alu_op(16) -> src1.asSInt() % src2.asSInt,
            alu_op(17) -> src1 % src2
        )
    )
}