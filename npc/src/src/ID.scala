import chisel3._
import chisel3.util._

class ID extends Module
{
    val io = IO(new Bundle
    {
        val IF_ID = Flipped(new IF_ID)
        val ID_EX = new ID_EX

        val reg_r = Flipped(new reg_r)
    })

    val decoder7128 = Module(new decoder(7, 128))
    val decoder38 = Module(new decoder(3, 8))
    decoder7128.io.in := io.IF_ID.inst(6, 0)
    decoder38.io.in := io.IF_ID.inst(14, 12)

    val inst_addi = decoder7128.io.out(0x13) & decoder38.io.out(0x0)

    io.ID_EX.alu.alu_op := 0.U.asBools()
    io.ID_EX.alu.alu_op(0) := inst_addi
    io.ID_EX.alu.alu_src1 := io.reg_r.rdata1
    io.ID_EX.alu.alu_src2 := io.reg_r.rdata2
}