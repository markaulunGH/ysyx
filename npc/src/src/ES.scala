import chisel3._
import chisel3.util._

class ES extends Module
{
    val io = IO(new Bundle
    {
        val ds_es = Flipped(new DS_ES)
        val es_ms = new ES_MS

        val mm_ren = Output(UInt(1.W))
        val mm_raddr = Output(UInt(64.W))
        val mm_wen = Output(UInt(1.W))
        val mm_waddr = Output(UInt(64.W))
        val mm_wdata = Output(UInt(64.W))
        val mm_mask = Output(UInt(8.W))
    })
    val alu = Module(new Alu)
    io.ds_es.alu_in <> alu.io.in
    val alu_result = Mux(io.ds_es.inst_word, Cat(Fill(32, alu.io.alu_result(31)), alu.io.alu_result(31, 0)), alu.io.alu_result)

    io.mm_ren := io.ds_es.mm_ren
    io.mm_raddr := alu_result
    io.mm_wen := io.ds_es.mm_wen
    io.mm_waddr := alu_result
    io.mm_wdata := io.ds_es.mm_wdata
    io.mm_mask := io.ds_es.mm_mask

    io.es_ms.alu_result := alu_result
    io.es_ms.rf_wen := io.ds_es.rf_wen
    io.es_ms.rf_waddr := io.ds_es.rf_waddr
    io.es_ms.mm_mask := io.ds_es.mm_mask
    io.es_ms.mm_unsigned := io.ds_es.mm_unsigned
    io.es_ms.res_from_mem := io.ds_es.res_from_mem

    io.es_ms.csr_wen := io.ds_es.csr_wen
    io.es_ms.csr_addr := io.ds_es.csr_addr
    io.es_ms.csr_wmask := io.ds_es.csr_wmask
    io.es_ms.csr_wdata := io.ds_es.csr_wdata
}