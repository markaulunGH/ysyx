import chisel3._
import chisel3.util._

class WS_DS extends Bundle
{
    val ws_valid = Output(Bool())
    val rf_wen = Output(Bool())
    val rf_waddr = Output(UInt(5.W))
    val rf_wdata = Output(UInt(64.W))
}

class WS_MS extends Bundle
{
    val ws_allow_in = Output(Bool())
}

class WS extends Module
{
    val ws_ds = IO(new WS_DS)
    val ws_ms = IO(new WS_MS)
    val ms_ws = IO(Flipped(new MS_WS))

    val rf_w = IO(Flipped(new Regfile_W))
    val csr_rw = IO(Flipped(new CSR_RW))

    val sim = IO(new Bundle
    {
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
        ws_valid := ms_ws.to_ws_valid
    }

    val ws_reg = RegEnable(ms_ws, ms_ws.to_ws_valid && ws_allow_in)

    rf_w.wen := ws_reg.rf_wen
    rf_w.waddr := ws_reg.rf_waddr
    rf_w.wdata := Mux(ws_reg.csr_wen, csr_rw.rdata, ws_reg.rf_wdata)

    csr_rw.addr := ws_reg.csr_addr
    csr_rw.wen := ws_reg.csr_wen
    csr_rw.wdata := (ws_reg.csr_wdata & ws_reg.csr_wmask) | (csr_rw.rdata & ~ws_reg.csr_wmask)
    csr_rw.pc := ws_reg.pc
    csr_rw.exc := ws_reg.exc
    csr_rw.exc_cause := ws_reg.exc_cause
    csr_rw.mret := ws_reg.mret

    ws_ds.ws_valid := ws_valid
    ws_ds.rf_wen := ws_reg.rf_wen
    ws_ds.rf_waddr := ws_reg.rf_waddr
    ws_ds.rf_wdata := Mux(ws_reg.csr_wen, csr_rw.rdata, ws_reg.rf_wdata)

    ws_ms.ws_allow_in := ws_allow_in

    sim.inst_end := ms_ws.to_ws_valid && ws_allow_in
    sim.pc := ws_reg.pc
    sim.inst := ms_ws.inst
    sim.ebreak := ws_reg.ebreak
}