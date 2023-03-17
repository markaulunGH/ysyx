import chisel3._
import chisel3.util._

class MS extends Module
{
    val io = IO(new Bundle
    {
        val es_mm = Flipped(new ES_MS)
        val ms_ws = new MS_WS

        val mm_rdata = Input(UInt(64.W))
    })

    io.ms_ws.rf_wen := io.es_mm.rf_wen
    io.ms_ws.rf_waddr := io.es_mm.rf_waddr
    io.ms_ws.rf_wdata := Mux(io.es_mm.res_from_mem, io.mm_rdata, io.es_mm.alu_result)
}