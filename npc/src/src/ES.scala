import chisel3._
import chisel3.util._

class ES extends Module
{
    val io = IO(new Bundle
    {
        val es_fs = new ES_FS
        val es_ds = new ES_DS
        val es_ms = new ES_MS
        val ds_es = Flipped(new DS_ES)
        val ms_es = Flipped(new MS_ES)

        val data_master = new AXI_Lite_Master
    })

    val arfire = RegInit(false.B)
    val wfire = RegInit(false.B)
    val mm_ren = Wire(Bool())
    val mm_wen = Wire(Bool())

    val es_valid = RegInit(false.B)
    val es_ready = (mm_ren && (io.data_master.ar.fire || arfire)) || (mm_wen && (io.data_master.w.fire || wfire)) || (!mm_ren && !mm_wen)
    val es_allow_in = !es_valid || es_ready && io.ms_es.ms_allow_in
    val to_ms_valid = es_valid && es_ready
    when (es_allow_in)
    {
        es_valid := io.ds_es.to_es_valid
    }

    val es_reg = RegEnable(io.ds_es, io.ds_es.to_es_valid && es_allow_in)
    mm_ren := es_reg.mm_ren
    mm_wen := es_reg.mm_wen

    val alu = Module(new Alu)
    // alu.io.in.alu_op := es_reg.alu_in.alu_op
    // alu.io.in.alu_src1 := es_reg.alu_in.alu_src1
    // alu.io.in.alu_src2 := es_reg.alu_in.alu_src2
    // alu.io.in.alu_op := Wire(Vec(18, 0.U(1.W)))
    alu.io.in.alu_src1 := 0.U(64.W)
    alu.io.in.alu_src2 := 0.U(64.W)
    val alu_result = Mux(es_reg.inst_word, Cat(Fill(32, alu.io.alu_result(31)), alu.io.alu_result(31, 0)), alu.io.alu_result)

    io.data_master.ar.valid := es_reg.mm_ren && !arfire && es_valid
    io.data_master.ar.bits.addr := alu_result
    io.data_master.ar.bits.prot := 0.U(3.W)
    when (es_allow_in)
    {
        arfire := false.B
    }
    .elsewhen (io.data_master.ar.fire)
    {
        arfire := true.B
    }

    val awfire = RegInit(false.B)
    io.data_master.aw.valid := es_reg.mm_wen && !awfire && es_valid
    io.data_master.aw.bits.addr := alu_result
    io.data_master.aw.bits.prot := 0.U(3.W)
    when (es_allow_in)
    {
        awfire := false.B
    }
    .elsewhen (io.data_master.aw.fire)
    {
        awfire := true.B
    }

    io.data_master.w.valid := es_reg.mm_wen && !wfire && es_valid
    io.data_master.w.bits.data := es_reg.mm_wdata
    io.data_master.w.bits.strb := es_reg.mm_mask
    when (es_allow_in)
    {
        wfire := false.B
    }
    .elsewhen (io.data_master.w.fire)
    {
        wfire := true.B
    }

    io.es_fs.es_allow_in := es_allow_in

    io.es_ds.es_allow_in := es_allow_in
    io.es_ds.es_valid := es_valid
    io.es_ds.to_ms_valid := to_ms_valid
    io.es_ds.alu_result := alu_result
    io.es_ds.rf_waddr := es_reg.rf_waddr
    io.es_ds.rf_wen := es_reg.rf_wen
    io.es_ds.mm_ren := es_reg.mm_ren
    io.es_ds.csr_wen := es_reg.csr_wen

    io.es_ms.to_ms_valid := to_ms_valid
    io.es_ms.pc := es_reg.pc

    io.es_ms.alu_result := alu_result
    io.es_ms.rf_wen := es_reg.rf_wen
    io.es_ms.rf_waddr := es_reg.rf_waddr
    io.es_ms.mm_ren := es_reg.mm_ren
    io.es_ms.mm_wen := es_reg.mm_wen
    io.es_ms.mm_mask := es_reg.mm_mask
    io.es_ms.mm_unsigned := es_reg.mm_unsigned

    io.es_ms.csr_wen := es_reg.csr_wen
    io.es_ms.csr_addr := es_reg.csr_addr
    io.es_ms.csr_wmask := es_reg.csr_wmask
    io.es_ms.csr_wdata := es_reg.csr_wdata
    io.es_ms.exc := es_reg.exc
    io.es_ms.exc_cause := es_reg.exc_cause
    io.es_ms.mret := es_reg.mret

    io.es_ms.inst := es_reg.inst
    io.es_ms.ebreak := es_reg.ebreak
}