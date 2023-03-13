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

    val imm_I = inst(31, 20)
    val imm_S = Cat(inst(31, 25), inst(11, 7))
    val imm_B = Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
    val imm_U = Cat(inst(31, 12), 0.U(12.W))
    val imm_J = Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W))
    val rs1 = inst(19, 15)
    val rs2 = inst(24, 20)
    val rd = inst(11, 7)
    val opcode = inst(6, 0)
    val funct3 = inst(14, 12)

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

    val src1_is_pc = inst_auipc
    val src2_is_imm = inst_auipc || inst_addi || inst_lui

    val imm = Wire(UInt(64.W))

    .when (inst_jalr === 1.U || inst_addi === 1.U)
        imm := imm_I
    // .elsewhen ()
    //     imm := imm_S
    // .elsewhen ()
    //     imm := imm_B
    .elsewhen (inst_lui || inst_auipc)
        imm := imm_U
    .elsewhen (inst_jal)
        imm := imm_J
    .otherwise
        imm := 0.U(64.W)

    io.reg_r.raddr1 := Mux(inst_lui, 0, rs1)
    io.reg_r.raddr2 := rs2

    for (i <- 0 until 19)
        io.ds_es.alu.alu_op(i) := 0.U
    io.ds_es.alu.alu_op(0) := inst_auipc || inst_addi || inst_lui
    io.ds_es.alu.alu_src1 := Mux(src1_is_pc, io.fs_ds.pc, io.reg_r.rdata1)
    io.ds_es.alu.alu_src2 := Mux(src2_is_imm, imm, io.reg_r.rdata2)

    io.ds_es.wen := inst_addi
    io.ds_es.waddr := rd
}