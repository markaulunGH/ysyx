import chisel3._
import chisel3.util._

class MS extends Module
{
    val io = IO(new Bundle
    {
        val es_mm = Flipped(new ES_MS)
        val ms_ws = new MS_WS
    })

    io.ms_ws.alu_result := io.es_mm.alu_result
    io.ms_ws.rf_wen := io.es_mm.rf_wen
    io.ms_ws.rf_waddr := io.es_mm.rf_waddr
}