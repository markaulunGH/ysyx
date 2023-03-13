import chisel3._
import chisel3.util._

class DS extends Module
{
    val io = IO(new Bundle
    {
        val fs_ds = Flipped(new FS_DS)
        val ds_es = new DS_ES

        val reg_r = Flipped(new reg_r)
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

    val inst_lui   = dopcode(0x37)
    val inst_auipc = dopcode(0x17)
    val inst_addi  = dopcode(0x13) & dfunct3(0x0)
    val inst_jal   = dopcode(0x6f)
    val inst_jalr  = dopcode(0x67)

    val src1_is_pc = inst_auipc || inst_jal || inst_jalr
    val src2_is_imm = inst_auipc || inst_addi || inst_lui

    val imm = Wire(UInt(64.W))
    when (inst_jalr || inst_addi)
    {
        imm := Cat(Fill(52, imm_I(11)), imm_I)
    }
    // .elsewhen ()
    // {
    //     imm := Cat(Fill(52, imm_S(11), imm_S)
    // }
    // .elsewhen ()
    // {
    //     imm := Cat(Fill(51, imm_B(12), imm_B)
    // }
    .elsewhen (inst_lui || inst_auipc)
    {
        imm := Cat(Fill(32, imm_U(31)), imm_U)
    }
    .elsewhen (inst_jal)
    {
        imm := Cat(Fill(43, imm_J(20)), imm_J)
    }
    .otherwise
    {
        imm := 0.U(64.W)
    }

    io.fs_ds.br_taken := inst_jal || inst_jalr
    when (inst_jal)
    {
        io.fs_ds.br_taken := io.fs_ds.pc + imm_J
    }
    .elsewhen (inst_jalr)
    {
        io.fs_ds.br_taken := io.fs_ds.pc + (io.reg_r.rdata2 & ~1.U)
    }
    .otherwise
    {
        io.fs_ds.br_taken := 0.U
    }

    io.reg_r.raddr1 := Mux(inst_lui, 0.U, rs1)
    io.reg_r.raddr2 := Mux(inst_jal || inst_jalr, 0.U, rs2)

    for (i <- 0 until 19)
    {
        io.ds_es.alu.alu_op(i) := 0.U
    }
    io.ds_es.alu.alu_op(0) := inst_auipc || inst_addi || inst_lui || inst_jal || inst_jalr
    io.ds_es.alu.alu_src1 := Mux(src1_is_pc, io.fs_ds.pc, io.reg_r.rdata1)
    io.ds_es.alu.alu_src2 := Mux(src2_is_imm, imm, io.reg_r.rdata2)

    io.ds_es.wen := inst_auipc || inst_jal || inst_jalr || inst_addi
    io.ds_es.waddr := rd
}