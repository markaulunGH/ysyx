import chisel3._
import chisel3.util._

class ES extends Module
{
    val io = IO(new Bundle
    {
        val ds_es = Flipped(new DS_ES)
        val es_ms = new ES_MS

        val data_master = new AXI_Lite_Master

        val es_ready = Output(Bool())
        val ready = Input(Bool())
    })

    val alu = Module(new Alu)
    io.ds_es.alu_in <> alu.io.in
    val alu_result = Mux(io.ds_es.inst_word, Cat(Fill(32, alu.io.alu_result(31)), alu.io.alu_result(31, 0)), alu.io.alu_result)


    io.data_master.ar.valid := io.ds_es.mm_ren
    io.data_master.ar.bits.addr := alu_result
    io.data_master.ar.bits.prot := 0.U(3.W)
    val arfire = RegInit(false.B)
    when (io.data_master.ar.fire)
    {
        arfire := true.B
    }
    .elsewhen (io.ready)
    {
        arfire := false.B
    }

    val awfire = RegInit(false.B)
    io.data_master.aw.valid := io.ds_es.mm_wen && !awfire
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
    io.data_master.w.valid := io.ds_es.mm_wen && awfire && !wfire
    io.data_master.w.bits.data := io.ds_es.mm_wdata
    io.data_master.w.bits.strb := io.ds_es.mm_mask
    when (io.data_master.w.fire)
    {
        wfire := true.B
    }
    .elsewhen (io.ready)
    {
        wfire := false.B
    }

    io.es_ms.pc := io.ds_es.pc

    io.es_ms.alu_result := alu_result
    io.es_ms.rf_wen := io.ds_es.rf_wen
    io.es_ms.rf_waddr := io.ds_es.rf_waddr
    io.es_ms.mm_ren := io.ds_es.mm_ren
    io.es_ms.mm_wen := io.ds_es.mm_wen
    io.es_ms.mm_mask := io.ds_es.mm_mask
    io.es_ms.mm_unsigned := io.ds_es.mm_unsigned

    io.es_ms.csr_wen := io.ds_es.csr_wen
    io.es_ms.csr_addr := io.ds_es.csr_addr
    io.es_ms.csr_wmask := io.ds_es.csr_wmask
    io.es_ms.csr_wdata := io.ds_es.csr_wdata
    io.es_ms.exc := io.ds_es.exc
    io.es_ms.exc_cause := io.ds_es.exc_cause
    io.es_ms.mret := io.ds_es.mret

    io.es_ready := (io.ds_es.mm_ren && arfire) || (io.ds_es.mm_wen && wfire) || (!io.ds_es.mm_ren && !io.ds_es.mm_wen)
}