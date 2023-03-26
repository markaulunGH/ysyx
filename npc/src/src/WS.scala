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
    io.reg_w.wdata := Mux(io.csr_wen, io.csr.csr_rdata, io.ms_ws.rf_wdata)
}