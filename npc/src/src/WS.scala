import chisel3._
import chisel3.util._

class WS extends Module
{
    val io = IO(new Bundle
    {
        val ws_ds = new WS_DS
        val ws_ms = new WS_MS
        val ms_ws = Flipped(new MS_WS)

        val reg_w = Flipped(new Reg_w)
        val csr_rw = Flipped(new CSR_rw)

        val inst_end = Output(Bool())
        val pc = Output(UInt(64.W))
        val inst = Output(UInt(32.W))
        val ebreak = Output(Bool())
    })

    val ws_valid = RegInit(false.B)
    val ws_ready = true.B
    val ws_allow_in = !ws_valid || ws_ready
    when (ws_allow_in)
    {
        ws_valid := io.ms_ws.to_ws_valid
    }

    val ws_reg = RegEnable(io.ms_ws, io.ms_ws.to_ws_valid && ws_allow_in)

    io.reg_w.wen := ws_reg.rf_wen
    io.reg_w.waddr := ws_reg.rf_waddr
    io.reg_w.wdata := Mux(ws_reg.csr_wen, io.csr_rw.rdata, ws_reg.rf_wdata)

    io.csr_rw.addr := ws_reg.csr_addr
    io.csr_rw.wen := ws_reg.csr_wen
    io.csr_rw.wdata := (ws_reg.csr_wdata & ws_reg.csr_wmask) | (io.csr_rw.rdata & ~ws_reg.csr_wmask)
    io.csr_rw.pc := ws_reg.pc
    io.csr_rw.exc := ws_reg.exc
    io.csr_rw.exc_cause := ws_reg.exc_cause
    io.csr_rw.mret := ws_reg.mret

    io.ws_ds.ws_valid := ws_valid
    io.ws_ds.rf_wen := ws_reg.rf_wen
    io.ws_ds.rf_waddr := ws_reg.rf_waddr
    io.ws_ds.rf_wdata := Mux(ws_reg.csr_wen, io.csr_rw.rdata, ws_reg.rf_wdata)

    io.ws_ms.ws_allow_in := ws_allow_in

    io.inst_end := io.ms_ws.to_ws_valid && ws_allow_in
    io.pc := ws_reg.pc
    io.inst := io.ms_ws.inst
    io.ebreak := ws_reg.ebreak
}