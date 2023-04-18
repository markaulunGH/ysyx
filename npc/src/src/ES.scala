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

    val enable = io.ds_es.to_es_valid && es_allow_in
    val pc = RegEnable(io.ds_es.pc, enable)
    val alu_op = RegEnable(io.ds_es.alu_in.alu_op, enable)
    val alu_src1 = RegEnable(io.ds_es.alu_in.alu_src1, enable)
    val alu_src2 = RegEnable(io.ds_es.alu_in.alu_src2, enable)
    val inst_word = RegEnable(io.ds_es.inst_word, enable)
    val rf_wen = RegEnable(io.ds_es.rf_wen, enable)
    val rf_waddr = RegEnable(io.ds_es.rf_waddr, enable)
    mm_ren := RegEnable(io.ds_es.mm_ren, enable)
    mm_wen := RegEnable(io.ds_es.mm_wen, enable)
    val mm_wdata = RegEnable(io.ds_es.mm_wdata, enable)
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
    alu.io.in.alu_op := alu_op
    alu.io.in.alu_src1 := alu_src1
    alu.io.in.alu_src2 := alu_src2
    val alu_result = Mux(inst_word, Cat(Fill(32, alu.io.alu_result(31)), alu.io.alu_result(31, 0)), alu.io.alu_result)

    io.data_master.ar.valid := mm_ren && !arfire && es_valid
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
    io.data_master.aw.valid := mm_wen && !awfire && es_valid
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

    io.data_master.w.valid := mm_wen && !wfire && es_valid
    io.data_master.w.bits.data := mm_wdata
    io.data_master.w.bits.strb := mm_mask
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
    io.es_ds.rf_waddr := rf_waddr
    io.es_ds.rf_wen := rf_wen
    io.es_ds.mm_ren := mm_ren
    io.es_ds.csr_wen := csr_wen

    io.es_ms.to_ms_valid := to_ms_valid
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

    val inst = RegEnable(io.ds_es.inst, enable)
    val ebreak = RegEnable(io.ds_es.ebreak, enable)
    io.es_ms.inst := inst
    io.es_ms.ebreak := ebreak
}