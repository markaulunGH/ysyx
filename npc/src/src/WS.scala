import chisel3._
import chisel3.util._

class WS extends Module
{
    val io = IO(new Bundle
    {
        val ms_ws = Flipped(new MS_WS)

        val reg_w = Flipped(new Reg_w)
        val csr = Flipped(new Csr_io)
    })

    io.reg_w.wen := io.ms_ws.rf_wen
    io.reg_w.waddr := io.ms_ws.rf_waddr
    io.reg_w.wdata := Mux(io.ms_ws.csr_wen, io.csr.rdata, io.ms_ws.rf_wdata)

    io.csr.addr := io.ms_ws.csr_addr
    io.csr.wen := io.ms_ws.csr_wen
    io.csr.wdata := (io.ms_ws.csr_wdata & io.ms_ws.csr_wmask) | (io.csr.rdata & ~io.ms_ws.csr_wmask)
    io.csr.exc := io.ms_ws.exc
    io.csr.exc_cause := io.ms_ws.exc_cause
    io.csr.mret := io.ms_ws.mret
}