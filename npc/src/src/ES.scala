import chisel3._
import chisel3.util._

class ES extends Module
{
    val io = IO(new Bundle
    {
        val es_ds = new ES_DS
        val es_ms = new ES_MS
        val ds_es = Flipped(new DS_ES)
        val ms_es = Flipped(new MS_ES)

        val data_master = new AXI_Lite_Master
    })

    val es_valid = RegInit(false.B)
    val es_ready = (io.ds_es.mm_ren && arfire) || (io.ds_es.mm_wen && wfire) || (!io.ds_es.mm_ren && !io.ds_es.mm_wen)
    val es_allow_in = !es_valid || es_ready && io.ms_es.ms_allow_in
    val to_ms_valid = es_valid && es_ready
    when (es_allow_in)
    {
        es_valid := io.ds_es.to_es_valid
    }

    val enable = io.ds_es.to_es_valid && es_allow_in
    val pc = RegEnable(io.ds_es.pc, enable)
    val alu_op = RegEnable(io.ds_es.alu_in.alu_op, enable)
    val alu_src1 = RegEnable(io.ds_es.alu_in.alu_src1, enable)
    val alu_src2 = RegEnable(io.ds_es.alu_in.alu_src2, enable)
    val inst_word = RegEnable(io.ds_es.inst_word, enable)
    val rf_wen = RegEnable(io.ds_es.rf_wen, enable)
    val rf_waddr = RegEnable(io.ds_es.rf_waddr, enable)
    val mm_ren = RegEnable(io.ds_es.mm_ren, enable)
    val mm_wen = RegEnable(io.ds_es.mm_wen, enable)
    val mm_mask = RegEnable(io.ds_es.mm_mask, enable)
    val mm_unsigned = RegEnable(io.ds_es.mm_unsigned, enable)
    val csr_wen = RegEnable(io.ds_es.csr_wen, enable)
    val csr_addr = RegEnable(io.ds_es.csr_addr, enable)
    val csr_wmask = RegEnable(io.ds_es.csr_wmask, enable)
    val csr_wdata = RegEnable(io.ds_es.csr_wdata, enable)
    val exc = RegEnable(io.ds_es.exc, enable)
    val exc_cause = RegEnable(io.ds_es.exc_cause, enable)
    val mret = RegEnable(io.ds_es.mret, enable)

    val alu = Module(new Alu)
    alu.alu_in.alu_op := alu_op
    alu.alu_in.alu_src1 := alu_src1
    alu.alu_in.alu_src2 := alu_src2
    val alu_result = Mux(inst_word, Cat(Fill(32, alu.io.alu_result(31)), alu.io.alu_result(31, 0)), alu.io.alu_result)

    val arfire = RegInit(false.B)
    io.data_master.ar.valid := mm_ren && !arfire
    io.data_master.ar.bits.addr := alu_result
    io.data_master.ar.bits.prot := 0.U(3.W)
    when (io.data_master.ar.fire)
    {
        arfire := true.B
    }
    .elsewhen (io.ready)
    {
        arfire := false.B
    }

    val awfire = RegInit(false.B)
    io.data_master.aw.valid := mm_wen && !awfire
    io.data_master.aw.bits.addr := alu_result
    io.data_master.aw.bits.prot := 0.U(3.W)
    when (io.data_master.aw.fire)
    {
        awfire := true.B
    }
    .elsewhen (io.ready)
    {
        awfire := false.B
    }

    val wfire = RegInit(false.B)
    io.data_master.w.valid := mm_wen && !wfire
    io.data_master.w.bits.data := mm_wdata
    io.data_master.w.bits.strb := mm_mask
    when (io.data_master.w.fire)
    {
        wfire := true.B
    }
    .elsewhen (io.ready)
    {
        wfire := false.B
    }

    io.es_ms.pc := pc

    io.es_ms.alu_result := alu_result
    io.es_ms.rf_wen := rf_wen
    io.es_ms.rf_waddr := rf_waddr
    io.es_ms.mm_ren := mm_ren
    io.es_ms.mm_wen := mm_wen
    io.es_ms.mm_mask := mm_mask
    io.es_ms.mm_unsigned := mm_unsigned

    io.es_ms.csr_wen := csr_wen
    io.es_ms.csr_addr := csr_addr
    io.es_ms.csr_wmask := csr_wmask
    io.es_ms.csr_wdata := csr_wdata
    io.es_ms.exc := exc
    io.es_ms.exc_cause := exc_cause
    io.es_ms.mret := mret
}