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
    decoder7128.io.in := io.inst(6, 0)
    decoder38.io.in := io.inst(14, 12)

    val inst_addi = decoder7128.io.out(0x13) & decoder38.io.out(0x0)

    io.ID_EX.alu_op(0) := inst_addi
    io.ID_EX.alu_src1 := Mux(src1_is_pc, pc, rdata1)
    io.ID_EX.alu_src2 := Mux(src2_is_imm, imm, rdata2)
}