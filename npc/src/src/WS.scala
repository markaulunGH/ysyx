import chisel3._
import chisel3.util._

class WS extends Module
{
    val io = IO(new Bundle
    {
        val ms_ws = Flipped(new MS_WS)

        val reg_w = Flipped(new Reg_w)
        val csr_rw = Flipped(new Csr_rw)
    })

    io.reg_w.wen := io.ms_ws.rf_wen
    io.reg_w.waddr := io.ms_ws.rf_waddr
    io.reg_w.wdata := Mux(io.ms_ws.csr_wen, io.csr_rw.rdata, io.ms_ws.rf_wdata)

    io.csr_rw.addr := io.ms_ws.csr_addr
    io.csr_rw.wen := io.ms_ws.csr_wen
    io.csr_rw.wdata := (io.ms_ws.csr_wdata & io.ms_ws.csr_wmask) | (io.csr_rw.rdata & ~io.ms_ws.csr_wmask)
    io.csr_rw.pc := io.ms_ws.pc
    io.csr_rw.exc := io.ms_ws.exc
    io.csr_rw.exc_cause := io.ms_ws.exc_cause
    io.csr_rw.mret := io.ms_ws.mret
}