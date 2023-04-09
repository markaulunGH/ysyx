import chisel3._
import chisel3.util._

class DS extends Module
{
    val io = IO(new Bundle
    {
        val ds_pf = new DS_PF
        val ds_es = new DS_ES
        val fs_ds = Flipped(new FS_DS)
        val es_ds = Flipped(new ES_DS)

        val reg_r = Flipped(new Reg_r)
        val csr_pc = Flipped(new Csr_pc)

        val ebreak = Output(Bool())
    })

    val read_rf1 = Wire(Bool())
    val read_rf2 = Wire(Bool())

    val ds_valid = RegInit(false.B)
    val ds_ready = !(read_rf1 && rf1_hazard || read_rf2 && rf2_hazard)
    val ds_allow_in = !ds_valid || ds_ready && io.es_ds.es_allow_in
    val to_es_valid = ds_valid && ds_ready
    when (io.ds_pf.br_taken && to_es_valid && io.es_ds.es_allow_in)
    {
        ds_valid := false.B
    }
    .elsewhen (ds_allow_in)
    {
        ds_valid := io.fs_ds.to_ds_valid
    }

    val enable = io.fs_ds.to_ds_valid && ds_allow_in
    val pc = RegEnable(io.fs_ds.pc, enable)
    val inst = RegEnable(io.fs_ds.inst, enable)

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
    val csr_addr = inst(31, 20)
    val uimm = inst(19, 15)

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
    val inst_lwu    = dopcode(0x3)  && dfunct3(0x6)
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
    val inst_slli   = dopcode(0x13) && dfunct3(0x1) && (inst(31, 26) === 0x0.U)
    val inst_srli   = dopcode(0x13) && dfunct3(0x5) && (inst(30, 26) === 0x0.U)
    val inst_srai   = dopcode(0x13) && dfunct3(0x5) && (inst(30, 26) === 0x10.U)
    val inst_addiw  = dopcode(0x1b) && dfunct3(0x0)
    val inst_slliw  = dopcode(0x1b) && dfunct3(0x1) && (inst(31, 25) === 0x0.U)
    val inst_srliw  = dopcode(0x1b) && dfunct3(0x5) && (inst(30, 25) === 0x0.U)
    val inst_sraiw  = dopcode(0x1b) && dfunct3(0x5) && (inst(30, 25) === 0x20.U)
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
    val inst_ecall  = inst === 0x73.U
    val inst_ebreak = inst === 0x100073.U

    val inst_csrrw  = dopcode(0x73) && dfunct3(0x1)
    val inst_csrrs  = dopcode(0x73) && dfunct3(0x2)
    val inst_csrrc  = dopcode(0x73) && dfunct3(0x3)
    val inst_csrrwi = dopcode(0x73) && dfunct3(0x5)
    val inst_csrrsi = dopcode(0x73) && dfunct3(0x6)
    val inst_csrrci = dopcode(0x73) && dfunct3(0x7)

    val inst_mret   = inst === 0x30200073.U

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

    val inst_valid = inst_lui || inst_auipc || inst_jal || inst_jalr ||
                     inst_beq || inst_bne || inst_blt || inst_bge || inst_bltu || inst_bgeu ||
                     inst_lb || inst_lh || inst_lw || inst_lbu || inst_lhu || inst_lwu || inst_ld || inst_sb || inst_sh || inst_sw || inst_sd ||
                     inst_addi || inst_slti || inst_sltiu || inst_xori || inst_ori || inst_andi || inst_slli || inst_srli || inst_srai || inst_addiw || inst_slliw || inst_srliw || inst_sraiw ||
                     inst_add || inst_sub || inst_sll || inst_slt || inst_sltu || inst_xor || inst_srl || inst_sra || inst_or || inst_and || inst_addw || inst_subw || inst_sllw || inst_srlw || inst_sraw ||
                     inst_ecall || inst_ebreak ||
                     inst_csrrw || inst_csrrs || inst_csrrc || inst_csrrwi || inst_csrrsi || inst_csrrci ||
                     inst_mul || inst_mulh || inst_mulhsu || inst_mulhu || inst_div || inst_divu || inst_rem || inst_remu || inst_mulw || inst_divw || inst_divuw || inst_remw || inst_remuw

    val inst_R = inst_add || inst_sub || inst_sll || inst_slt || inst_sltu || inst_xor || inst_srl || inst_sra || inst_or || inst_and || inst_addw || inst_subw || inst_sllw || inst_srlw || inst_sraw ||
                 inst_mul || inst_mulh || inst_mulhsu || inst_mulhu || inst_div || inst_divu || inst_rem || inst_remu || inst_mulw || inst_divw || inst_divuw || inst_remw || inst_remuw
    val inst_I = inst_jalr ||
                 inst_lb || inst_lh || inst_lw || inst_lbu || inst_lhu || inst_lwu || inst_ld ||
                 inst_addi || inst_slti || inst_sltiu || inst_xori || inst_ori || inst_andi || inst_slli || inst_srli || inst_srai || inst_addiw || inst_slliw || inst_srliw || inst_sraiw
    val inst_S = inst_sb || inst_sh || inst_sw || inst_sd
    val inst_B = inst_beq || inst_bne || inst_blt || inst_bge || inst_bltu || inst_bgeu
    val inst_U = inst_lui || inst_auipc
    val inst_J = inst_jal

    val inst_load = inst_lb || inst_lh || inst_lw || inst_lbu || inst_lhu || inst_lwu || inst_ld
    val inst_store = inst_sb || inst_sh || inst_sw || inst_sd
    val inst_csr = inst_csrrw || inst_csrrs || inst_csrrc || inst_csrrwi || inst_csrrsi || inst_csrrci

    val src1_is_pc = inst_auipc || inst_jal || inst_jalr
    val src2_is_imm = inst_lui || inst_auipc ||
                      inst_jal || inst_jalr ||
                      inst_load || inst_store ||
                      inst_addi || inst_slti || inst_sltiu || inst_xori || inst_ori || inst_andi || inst_slli || inst_srli || inst_srai || inst_addiw || inst_slliw || inst_srliw || inst_sraiw

    val imm = MuxCase(
        0.U(64.W),
        Seq(
            inst_I -> Cat(Fill(52, imm_I(11)), imm_I),
            inst_S -> Cat(Fill(52, imm_S(11)), imm_S),
            inst_B -> Cat(Fill(51, imm_B(12)), imm_B),
            inst_U -> Cat(Fill(32, imm_U(31)), imm_U),
            inst_J -> Cat(Fill(43, imm_J(20)), imm_J)
        )
    )

    val mm_mask = MuxCase(
        0.U(8.W),
        Seq(
            (inst_lb || inst_lbu || inst_sb) -> 0x1.U(8.W),
            (inst_lh || inst_lhu || inst_sh) -> 0x3.U(8.W),
            (inst_lw || inst_lwu || inst_sw) -> 0xf.U(8.W),
            (inst_ld || inst_sd) -> 0xff.U(8.W)
        )
    )

    io.reg_r.raddr1 := Mux(inst_lui, 0.U, rs1)
    io.reg_r.raddr2 := rs2

    read_rf1 := inst_R || inst_I || inst_S || inst_B
    read_rf2 := inst_R || inst_S || inst_B

    val rs1_value = MuxCase(io.reg_r.rdata1, Seq(
        (io.reg_r.raddr1 =/= 0.U && io.es_ds.to_mm_valid && io.es_ds.rf_wen && io.reg_r.raddr1 === io.es_ds.rf_waddr) -> io.es_ds.alu_result
        (io.reg_r.raddr1 =/= 0.U && io.ms_ds.to_ws_valid && io.ms_ds.rf_wen && io.reg_r.raddr1 === io.ms_ds.rf_waddr) -> io.ms_ds.rf_wdata
        (io.reg_r.raddr1 =/= 0.U && io.ws_ds.valid       && io.ws_ds.rf_wen && io.reg_r.raddr1 === io.ws_ds.rf_waddr) -> io.ws_ds.rf_wdata
    ))
    val rs2_value = MuxCase(io.reg_r.rdata2, Seq(
        (io.reg_r.raddr2 && io.es_ds.to_mm_valid && io.es_ds.rf_wen && io.reg_r.raddr2 === io.es_ds.rf_waddr) -> io.es_ds.alu_result
        (io.reg_r.raddr2 && io.ms_ds.to_ws_valid && io.ms_ds.rf_wen && io.reg_r.raddr2 === io.ms_ds.rf_waddr) -> io.ms_ds.rf_wdata
        (io.reg_r.raddr2 && io.ws_ds.valid       && io.ws_ds.rf_wen && io.reg_r.raddr2 === io.ws_ds.rf_waddr) -> io.ws_ds.rf_wdata
    ))

    val rf1_hazard = (io.es_ds.es_valid && io.es_ds.mm_ren && io.reg_r.raddr1 === io.es_ds.rf_waddr) && io.reg_r.raddr1 =/= 0.U
    val rf2_hazard = (io.es_ds.es_valid && io.es_ds.mm_ren && io.reg_r.raddr2 === io.es_ds.rf_waddr) && io.reg_r.raddr2 =/= 0.U

    val rs1_lt_rs2 = rs1_value.asSInt < rs2_value.asSInt
    val rs1_ltu_rs2 = rs1_value < rs2_value
    io.ds_pf.br_taken := inst_jal || inst_jalr ||
                         inst_beq  &&  rs1_value === rs2_value ||
                         inst_bne  &&  rs1_value =/= rs2_value ||
                         inst_blt  &&  rs1_lt_rs2 ||
                         inst_bge  && !rs1_lt_rs2 ||
                         inst_bltu &&  rs1_ltu_rs2 ||
                         inst_bgeu && !rs1_ltu_rs2 ||
                         inst_ecall || inst_mret
    io.ds_pf.br_target := MuxCase(
        0.U(64.W),
        Seq(
            (inst_jal || inst_beq || inst_bne || inst_blt || inst_bge || inst_bltu || inst_bgeu) -> (pc + imm),
            inst_jalr -> (imm + Cat(io.reg_r.rdata1(63, 1), 0.U(1.W))),
            inst_ecall -> io.csr_pc.mtvec,
            inst_mret -> io.csr_pc.mepc
        )
    )

    val alu_op = Wire(Vec(18, UInt(1.W)))
    alu_op(0)  := inst_lui || inst_auipc || inst_jal || inst_jalr || inst_load || inst_store || inst_addi || inst_addiw || inst_add || inst_addw
    alu_op(1)  := inst_sub || inst_subw
    alu_op(2)  := inst_slti || inst_slt
    alu_op(3)  := inst_sltiu || inst_sltu
    alu_op(4)  := inst_xori || inst_xor
    alu_op(5)  := inst_ori || inst_or
    alu_op(6)  := inst_andi || inst_and
    alu_op(7)  := inst_slli || inst_slliw || inst_sll || inst_sllw
    alu_op(8)  := inst_srli || inst_srliw || inst_srl || inst_srlw
    alu_op(9)  := inst_srai || inst_sraiw || inst_sra || inst_sraw
    alu_op(10) := inst_mul || inst_mulw
    alu_op(11) := inst_mulh
    alu_op(12) := inst_mulhsu
    alu_op(13) := inst_mulhu
    alu_op(14) := inst_div || inst_divw
    alu_op(15) := inst_divu || inst_divuw
    alu_op(16) := inst_rem || inst_remw
    alu_op(17) := inst_remu || inst_remuw

    io.ds_es.pc := pc

    io.ds_es.alu_in.alu_op := alu_op
    io.ds_es.alu_in.alu_src1 := Mux(src1_is_pc, pc, 
        MuxCase(
            rs1_value,
            Seq(
                (inst_addiw || inst_slliw || inst_sraiw || inst_addw || inst_subw || inst_sllw || inst_sraw || inst_mulw || inst_divw || inst_remw) -> Cat(Fill(32, rs1_value(31)), rs1_value(31, 0)),
                (inst_srlw || inst_srliw || inst_divuw || inst_remuw) -> rs1_value(31, 0)
            )
        )
    )
    io.ds_es.alu_in.alu_src2 := Mux(src2_is_imm,
        MuxCase(
            imm,
            Seq(
                (inst_jal || inst_jalr) -> 4.U,
                (inst_slli || inst_srli || inst_srai) -> imm(5, 0),
                (inst_slliw || inst_srliw || inst_sraiw) -> imm(4, 0)
            )
        ),
        MuxCase(
            rs2_value,
            Seq(
                (inst_addw || inst_subw || inst_mulw || inst_divw || inst_remw) -> Cat(Fill(32, rs2_value(31)), rs2_value(31, 0)),
                (inst_divuw || inst_remuw) -> rs2_value(31, 0),
                (inst_sllw || inst_sraw || inst_srlw) -> rs2_value(4, 0)
            )
        )
    )
    io.ds_es.inst_word := inst_addiw || inst_slliw || inst_srliw || inst_sraiw || inst_addw || inst_subw || inst_sllw || inst_srlw || inst_sraw || inst_mulw || inst_divw || inst_divuw || inst_remw || inst_remuw

    io.ds_es.rf_wen := inst_lui || inst_auipc || inst_jal || inst_jalr ||
                       inst_load ||
                       inst_addi || inst_slti | inst_sltiu || inst_xori || inst_ori || inst_andi || inst_slli || inst_srli || inst_srai || inst_addiw || inst_slliw || inst_srliw || inst_sraiw ||
                       inst_add || inst_sub || inst_sll || inst_slt || inst_sltu || inst_xor || inst_srl || inst_sra || inst_or || inst_and ||
                       inst_addw || inst_subw || inst_sllw || inst_srlw || inst_sraw ||
                       inst_csr ||
                       inst_mul || inst_mulh || inst_mulhsu || inst_mulhu || inst_div || inst_divu || inst_rem || inst_remu ||
                       inst_mulw || inst_divw || inst_divuw || inst_remw || inst_remuw
    io.ds_es.rf_waddr := rd

    io.ds_es.mm_ren := inst_load
    io.ds_es.mm_wen := inst_store
    io.ds_es.mm_wdata := MuxCase(
        0.U(64.W),
        Seq(
            inst_sb -> rs2_value(7, 0),
            inst_sh -> rs2_value(15, 0),
            inst_sw -> rs2_value(31, 0),
            inst_sd -> rs2_value
        )
    )
    io.ds_es.mm_mask := mm_mask
    io.ds_es.mm_unsigned := inst_lbu || inst_lhu || inst_lwu

    io.ds_es.csr_wen := inst_csr
    io.ds_es.csr_addr := csr_addr
    io.ds_es.csr_wmask := MuxCase(
        0.U(64.W),
        Seq(
            (inst_csrrw || inst_csrrwi) -> Fill(64, 1.U(1.W)),
            (inst_csrrs || inst_csrrc) -> rs1_value,
            (inst_csrrsi || inst_csrrci) -> uimm
        )
    )
    io.ds_es.csr_wdata := MuxCase(
        0.U(64.W),
        Seq(
            inst_csrrw -> rs1_value,
            inst_csrrwi -> uimm,
            (inst_csrrs || inst_csrrsi) -> 1.U(64.W),
            (inst_csrrc || inst_csrrci) -> 0.U(64.W)
        )
    )
    io.ds_es.exc := inst_ecall
    io.ds_es.exc_cause := 0xb.U
    io.ds_es.mret := inst_mret

    io.ebreak := inst_ebreak
}