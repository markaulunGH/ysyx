import chisel3._
import chisel3.util._

class MS extends Module
{
    val io = IO(new Bundle
    {
        val es_ms = Flipped(new ES_MS)
        val ms_ws = new MS_WS

        val mm_rdata = Input(UInt(64.W))

        val valid = Output(Bool())
    })

    val mm_rdata = MuxCase(
        0.U(64.W),
        Seq(
            (io.es_ms.mm_mask === 0x1.U)  -> Cat(Fill(56, Mux(io.es_ms.mm_unsigned, 0.U(1.W), io.mm_rdata(7))),  io.mm_rdata(7, 0)),
            (io.es_ms.mm_mask === 0x3.U)  -> Cat(Fill(48, Mux(io.es_ms.mm_unsigned, 0.U(1.W), io.mm_rdata(15))), io.mm_rdata(15, 0)),
            (io.es_ms.mm_mask === 0xf.U)  -> Cat(Fill(32, Mux(io.es_ms.mm_unsigned, 0.U(1.W), io.mm_rdata(31))), io.mm_rdata(31, 0)),
            (io.es_ms.mm_mask === 0xff.U) -> io.mm_rdata
        )
    )

    io.ms_ws.pc := io.es_ms.pc

    io.ms_ws.rf_wen := io.es_ms.rf_wen
    io.ms_ws.rf_waddr := io.es_ms.rf_waddr
    io.ms_ws.rf_wdata := Mux(io.es_ms.res_from_mem, mm_rdata, io.es_ms.alu_result)

    io.ms_ws.csr_wen := io.es_ms.csr_wen
    io.ms_ws.csr_addr := io.es_ms.csr_addr
    io.ms_ws.csr_wdata := io.es_ms.csr_wdata
    io.ms_ws.csr_wmask := io.es_ms.csr_wmask
    io.ms_ws.exc := io.es_ms.exc
    io.ms_ws.exc_cause := io.es_ms.exc_cause
    io.ms_ws.mret := io.es_ms.mret
}