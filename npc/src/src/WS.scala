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

        val ws_valid = Output(Bool())
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

    val enable = io.ms_ws.to_ws_valid && ws_allow_in
    val pc = RegEnable(io.ms_ws.pc, enable)
    val rf_wen = RegEnable(io.ms_ws.rf_wen, enable)
    val rf_waddr = RegEnable(io.ms_ws.rf_waddr, enable)
    val rf_wdata = RegEnable(io.ms_ws.rf_wdata, enable)
    val csr_wen = RegEnable(io.ms_ws.csr_wen, enable)
    val csr_addr = RegEnable(io.ms_ws.csr_addr, enable)
    val csr_wmask = RegEnable(io.ms_ws.csr_wmask, enable)
    val csr_wdata = RegEnable(io.ms_ws.csr_wdata, enable)
    val exc = RegEnable(io.ms_ws.exc, enable)
    val exc_cause = RegEnable(io.ms_ws.exc_cause, enable)
    val mret = RegEnable(io.ms_ws.mret, enable)

    io.reg_w.wen := rf_wen
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
    io.ws_ds.rf_wdata := rf_wdata

    io.ws_ms.ws_allow_in := ws_allow_in

    val inst = RegEnable(io.ms_ws.inst, enable)
    val ebreak = RegEnable(io.ms_ws.ebreak, enable)
    io.inst_end := enable
    io.pc := pc
    io.inst := io.ms_ws.inst
    io.ebreak := ebreak
}