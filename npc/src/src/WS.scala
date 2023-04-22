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
        val csr_rw = Flipped(new Csr_rw)

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
    io.reg_w.waddr := rf_waddr
    io.reg_w.wdata := Mux(csr_wen, io.csr_rw.rdata, rf_wdata)

    io.csr_rw.addr := csr_addr
    io.csr_rw.wen := csr_wen
    io.csr_rw.wdata := (csr_wdata & csr_wmask) | (io.csr_rw.rdata & ~csr_wmask)
    io.csr_rw.pc := pc
    io.csr_rw.exc := exc
    io.csr_rw.exc_cause := exc_cause
    io.csr_rw.mret := mret

    io.ws_ds.ws_valid := ws_valid
    io.ws_ds.rf_wen := rf_wen
    io.ws_ds.rf_waddr := rf_waddr
    io.ws_ds.rf_wdata := Mux(csr_wen, io.csr_rw.rdata, rf_wdata)

    io.ws_ms.ws_allow_in := ws_allow_in

    val inst = RegEnable(io.ms_ws.inst, enable)
    val ebreak = RegEnable(io.ms_ws.ebreak, enable)
    io.inst_end := enable
    io.pc := pc
    io.inst := io.ms_ws.inst
    io.ebreak := ebreak
}