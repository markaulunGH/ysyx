import chisel3._
import chisel3.util._

class MS extends Module
{
    val io = IO(new Bundle
    {
        val es_ms = Flipped(new ES_MS)
        val ms_ws = new MS_WS

        val mm_rdata = Input(UInt(64.W))
    })

    val mask_rdata = MuxCase(
        0.U(64.W),
        Seq(
            es_ms.mm_mask == 0x1.U -> Cat()
        )
    )

    val mm_rdata = MuxCase(
        0.U(64.W),
        Seq(
            es_ms.mm_mask === 0x1.U  -> Cat(Fill(Mux(es_ms.mm_unsign, 0.U(1.W), io.mm_rdata(7)), 56),  io.mm_rdata(7, 0)),
            es_ms.mm_mask === 0x3.U  -> Cat(Fill(Mux(es_ms.mm_unsign, 0.U(1.W), io.mm_rdata(15)), 48), io.mm_rdata(15, 0)),
            es_ms.mm_mask === 0xf.U  -> Cat(Fill(Mux(es_ms.mm_unsign, 0.U(1.W), io.mm_rdata(31)), 32), io.mm_rdata(31, 0)),
            es_ms.mm_mask === 0xff.U -> io.mm_rdata
        )
    )

    io.ms_ws.rf_wen := io.es_ms.rf_wen
    io.ms_ws.rf_waddr := io.es_ms.rf_waddr
    io.ms_ws.rf_wdata := Mux(io.es_ms.res_from_mem, io.mm_rdata, io.es_ms.alu_result)
}