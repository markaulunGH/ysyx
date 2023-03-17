import chisel3._
import chisel3.util._

class DS extends Module
{
    val io = IO(new Bundle
    {
        val fs_ds = Flipped(new FS_DS)
        val ds_es = new DS_ES

        val reg_r = Flipped(new reg_r)

        val ebreak = Output(Bool())
    })

    val inst = io.fs_ds.inst
    val opcode = inst(6, 0)
    val funct3 = inst(14, 12)
    val funct7 = inst(31, 25)
    val rs1 = inst(19, 15)
    val rs2 = inst(24, 20)
    val rd = inst(11, 7)
    val imm_I = inst(31, 20)
    val imm_S = Cat(inst(31, 25), inst(11, 7))
    val imm_B = Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
    val imm_U = Cat(inst(31, 12), 0.U(12.W))
    val imm_J = Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W))

    val dopcode = UIntToOH(opcode)
    val dfunct3 = UIntToOH(funct3)
    val dfunct7 = UIntToOH(funct7)

    val inst_lui    = dopcode(0x37)
    val inst_auipc  = dopcode(0x17)
    val inst_jal    = dopcode(0x6f)
    val inst_jalr   = dopcode(0x67)
    val inst_beq    = dopcode(0x63) && dfunct3(0x0)
    val inst_bne    = dopcode(0x63) && dfunct3(0x1)
    val inst_blt    = dopcode(0x63) && dfunct3(0x4)
    val inst_bge    = dopcode(0x63) && dfunct3(0x5)
    val inst_bltu   = dopcode(0x63) && dfunct3(0x6)
    val inst_bgeu   = dopcode(0x63) && dfunct3(0x7)
    val inst_lb     = dopcode(0x3)  && dfunct3(0x0)
    val inst_lh     = dopcode(0x3)  && dfunct3(0x1)
    val inst_lw     = dopcode(0x3)  && dfunct3(0x2)
    val inst_lbu    = dopcode(0x3)  && dfunct3(0x4)
    val inst_lhu    = dopcode(0x3)  && dfunct3(0x5)
    val inst_lwu    = dopcode(0x3)  && dfunct3(0x7)
    val inst_ld     = dopcode(0x3)  && dfunct3(0x3)
    val inst_sb     = dopcode(0x23) && dfunct3(0x0)
    val inst_sh     = dopcode(0x23) && dfunct3(0x1)
    val inst_sw     = dopcode(0x23) && dfunct3(0x2)
    val inst_sd     = dopcode(0x23) && dfunct3(0x3)
    val inst_addi   = dopcode(0x13) && dfunct3(0x0)
    val inst_slti   = dopcode(0x13) && dfunct3(0x2)
    val inst_sltiu  = dopcode(0x13) && dfunct3(0x3)
    val inst_xori   = dopcode(0x13) && dfunct3(0x4)
    val inst_ori    = dopcode(0x13) && dfunct3(0x6)
    val inst_andi   = dopcode(0x13) && dfunct3(0x7)
    val inst_slli   = dopcode(0x13) && dfunct3(0x1)
    val inst_srli   = dopcode(0x13) && dfunct3(0x5) && !inst(30)
    val inst_srai   = dopcode(0x13) && dfunct3(0x5) &&  inst(30)
    val inst_addiw  = dopcode(0x1b) && dfunct3(0x0)
    val inst_slliw  = dopcode(0x1b) && dfunct3(0x1)
    val inst_srliw  = dopcode(0x1b) && dfunct3(0x5) && !inst(30)
    val inst_sraiw  = dopcode(0x1b) && dfunct3(0x5) &&  inst(30)
    val inst_add    = dopcode(0x33) && dfunct3(0x0) && dfunct7(0x0)
    val inst_sub    = dopcode(0x33) && dfunct3(0x0) && dfunct7(0x20)
    val inst_sll    = dopcode(0x33) && dfunct3(0x1) && dfunct7(0x0)
    val inst_slt    = dopcode(0x33) && dfunct3(0x2) && dfunct7(0x0)
    val inst_sltu   = dopcode(0x33) && dfunct3(0x3) && dfunct7(0x0)
    val inst_xor    = dopcode(0x33) && dfunct3(0x4) && dfunct7(0x0)
    val inst_srl    = dopcode(0x33) && dfunct3(0x5) && dfunct7(0x0)
    val inst_sra    = dopcode(0x33) && dfunct3(0x5) && dfunct7(0x20)
    val inst_or     = dopcode(0x33) && dfunct3(0x6) && dfunct7(0x0)
    val inst_and    = dopcode(0x33) && dfunct3(0x7) && dfunct7(0x0)
    val inst_addw   = dopcode(0x3b) && dfunct3(0x0) && dfunct7(0x0)
    val inst_subw   = dopcode(0x3b) && dfunct3(0x0) && dfunct7(0x20)
    val inst_sllw   = dopcode(0x3b) && dfunct3(0x1) && dfunct7(0x0)
    val inst_srlw   = dopcode(0x3b) && dfunct3(0x5) && dfunct7(0x0)
    val inst_sraw   = dopcode(0x3b) && dfunct3(0x5) && dfunct7(0x20)
    val inst_ecall  = dopcode(0x73) && !inst(20)
    val inst_ebreak = dopcode(0x73) &&  inst(20)

    val inst_mul    = dopcode(0x33) && dfunct3(0x0) && dfunct7(0x1)
    val inst_mulh   = dopcode(0x33) && dfunct3(0x1) && dfunct7(0x1)
    val inst_mulhsu = dopcode(0x33) && dfunct3(0x2) && dfunct7(0x1)
    val inst_mulhu  = dopcode(0x33) && dfunct3(0x3) && dfunct7(0x1)
    val inst_div    = dopcode(0x33) && dfunct3(0x4) && dfunct7(0x1)
    val inst_divu   = dopcode(0x33) && dfunct3(0x5) && dfunct7(0x1)
    val inst_rem    = dopcode(0x33) && dfunct3(0x6) && dfunct7(0x1)
    val inst_remu   = dopcode(0x33) && dfunct3(0x7) && dfunct7(0x1)
    val inst_mulw   = dopcode(0x3b) && dfunct3(0x0) && dfunct7(0x1)
    val inst_divw   = dopcode(0x3b) && dfunct3(0x4) && dfunct7(0x1)
    val inst_divuw  = dopcode(0x3b) && dfunct3(0x5) && dfunct7(0x1)
    val inst_remw   = dopcode(0x3b) && dfunct3(0x6) && dfunct7(0x1)
    val inst_remuw  = dopcode(0x3b) && dfunct3(0x7) && dfunct7(0x1)
    
    io.ebreak := inst_ebreak

    val src1_is_pc = inst_auipc || inst_jal || inst_jalr
    val src2_is_imm = inst_lui || inst_auipc || inst_jal || inst_jalr || inst_addi

    val imm = MuxCase(
        0.U(64.W),
        Seq(
            (inst_jalr || inst_addi) -> Cat(Fill(52, imm_I(11)), imm_I),
            false.B -> Cat(Fill(52, imm_S(11)), imm_S),
            (inst_beq || inst_bne || inst_blt || inst_bge || inst_bltu || inst_bgeu) -> Cat(Fill(51, imm_B(12)), imm_B),
            (inst_lui || inst_auipc) -> Cat(Fill(32, imm_U(31)), imm_U),
            inst_jal -> Cat(Fill(43, imm_J(20)), imm_J)
        )
    )

    io.reg_r.raddr1 := Mux(inst_lui, 0.U, rs1)
    io.reg_r.raddr2 := rs2
    val rs1_value = io.reg_r.rdata1
    val rs2_value = io.reg_r.rdata2

    val rs1_lt_rs2 = rs1_value.asSInt < rs2_value.asSInt
    val rs1_ltu_rs2 = rs1_value < rs2_value
    io.fs_ds.br_taken := inst_jal || inst_jalr ||
                         inst_beq  &&  rs1_value === rs2_value ||
                         inst_bne  &&  rs1_value =/= rs2_value ||
                         inst_blt  &&  rs1_lt_rs2 ||
                         inst_bge  && !rs1_lt_rs2 ||
                         inst_bltu &&  rs1_ltu_rs2 ||
                         inst_bgeu && !rs1_ltu_rs2
    io.fs_ds.br_target := MuxCase(
        0.U(64.W),
        Seq(
            (inst_jal || inst_beq || inst_bne || inst_blt || inst_bge || inst_bltu || inst_bgeu) -> (io.fs_ds.pc + imm),
            inst_jalr -> (imm + Cat(io.reg_r.rdata1(63, 1), 0.U(1.W))),
        )
    )

    for (i <- 0 until 19)
    {
        io.ds_es.alu.alu_op(i) := 0.U
    }
    io.ds_es.alu.alu_op(0) := inst_lui || inst_auipc || inst_jal || inst_jalr || inst_addi
    io.ds_es.alu.alu_src1 := Mux(src1_is_pc, io.fs_ds.pc, io.reg_r.rdata1)
    io.ds_es.alu.alu_src2 := Mux(src2_is_imm, Mux(inst_jal || inst_jalr, 4.U, imm), io.reg_r.rdata2)

    io.ds_es.rf_wen := inst_auipc || inst_jal || inst_jalr || inst_addi
    io.ds_es.rf_waddr := rd
}