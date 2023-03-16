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
    val rs1 = inst(19, 15)
    val rs2 = inst(24, 20)
    val rd = inst(11, 7)
    val imm_I = inst(31, 20)
    val imm_S = Cat(inst(31, 25), inst(11, 7))
    val imm_B = Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
    val imm_U = Cat(inst(31, 12), 0.U(12.W))
    val imm_J = Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W))

    val decoder7128 = Module(new Decoder(7, 128))
    val decoder38 = Module(new Decoder(3, 8))
    decoder7128.io.in := opcode
    val dopcode = decoder7128.io.out
    decoder38.io.in := funct3
    val dfunct3 = decoder38.io.out

    // val dopcode = 

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
    // val inst_addiw  = dopcode()
    // val inst_slliw  = dopcode()
    // val inst_srliw  = dopcode()
    // val inst_sraiw  = dopcode()
    // val inst_add    = dopcode()
    // val inst_sub    = dopcode()
    // val inst_sll    = dopcode()
    // val inst_slt    = dopcode()
    // val inst_sltu   = dopcode()
    // val inst_xor    = dopcode()
    // val inst_srl    = dopcode()
    // val inst_sra    = dopcode()
    // val inst_or     = dopcode()
    // val inst_and    = dopcode()
    // val inst_addw   = dopcode()
    // val inst_subw   = dopcode()
    // val inst_sllw   = dopcode()
    // val inst_srlw   = dopcode()
    // val inst_sraw   = dopcode()

    // val inst_mul    = dopcode()
    // val inst_mulh   = dopcode()
    // val inst_mulhsu = dopcode()
    // val inst_mulhu  = dopcode()
    // val inst_div    = dopcode()
    // val inst_divu   = dopcode()
    // val inst_rem    = dopcode()
    // val inst_remu   = dopcode()
    // val inst_mulw   = dopcode()
    // val inst_divw   = dopcode()
    // val inst_divuw  = dopcode()
    // val inst_remw   = dopcode()
    // val inst_remuw  = dopcode()
    
    val inst_ebreak = dopcode(0x73) && inst(31, 20) === 0x1.U
    io.ebreak := inst_ebreak

    val src1_is_pc = inst_auipc || inst_jal || inst_jalr
    val src2_is_imm = inst_lui || inst_auipc || inst_jal || inst_jalr || inst_addi

    val imm = Wire(UInt(64.W))
    
    imm := MuxCase
    (
        0.U(64.W),
        Array
        (
            (inst_jal || inst_addi) -> Cat(Fill(52, imm_I(11)), imm_I),
            // False.Bool -> Cat(Fill(52, imm_S(11), imm_S)),
            // False -> Cat(Fill(51, imm_B(12), imm_B)),
            (inst_lui || inst_auipc) -> Cat(Fill(32, imm_U(31)), imm_U),
            inst_jal -> Cat(Fill(43, imm_J(20)), imm_J)
        )
    )

    io.fs_ds.br_taken := inst_jal || inst_jalr
    when (inst_jal)
    {
        io.fs_ds.br_target := io.fs_ds.pc + imm
    }
    .elsewhen (inst_jalr)
    {
        io.fs_ds.br_target := imm + Cat(io.reg_r.rdata1(63, 1), 0.U(1.W))
    }
    .otherwise
    {
        io.fs_ds.br_target := 0.U
    }

    io.reg_r.raddr1 := Mux(inst_lui, 0.U, rs1)
    io.reg_r.raddr2 := rs2

    for (i <- 0 until 19)
    {
        io.ds_es.alu.alu_op(i) := 0.U
    }
    io.ds_es.alu.alu_op(0) := inst_auipc || inst_addi || inst_lui || inst_jal || inst_jalr
    io.ds_es.alu.alu_src1 := Mux(src1_is_pc, io.fs_ds.pc, io.reg_r.rdata1)
    io.ds_es.alu.alu_src2 := Mux(src2_is_imm, Mux(inst_jal || inst_jalr, 4.U, imm), io.reg_r.rdata2)

    io.ds_es.wen := inst_auipc || inst_jal || inst_jalr || inst_addi
    io.ds_es.waddr := rd
}