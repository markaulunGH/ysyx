import chisel3._
import chisel3.util._

class MS extends Module
{
    val io = IO(new Bundle
    {
        val ms_es = new MS_ES
        val ms_ws = new MS_WS
        val es_ms = Flipped(new ES_MS)
        val ws_ms = Flipped(new WS_MS)

        val data_slave = new AXI_Lite_Slave
    })

    val ms_valid = RegInit(false.B)
    val ms_ready = (io.es_ms.mm_ren && (io.data_slave.r.fire || rfire)) || (io.es_ms.mm_wen && (io.data_slave.b.fire || bfire)) || (!io.es_ms.mm_ren && !io.es_ms.mm_wen)
    val ms_allow_in = !ms_valid || ms_ready && io.ws_ms.ws_allow_in
    val to_ws_valid = ms_valid && ms_ready
    when (ms_allow_in)
    {
        ms_valid := io.es_ms.to_ms_valid
    }

    val enable = io.es_ms.to_ms_valid && ms_allow_in
    val pc = RegEnable(io.es_ms.pc, enable)
    val alu_result = RegEnable(io.es_ms.alu_result, enable)
    val rf_wen = RegEnable(io.es_ms.rf_wen, enable)
    val rf_waddr = RegEnable(io.es_ms.rf_waddr, enable)
    val mm_ren = RegEnable(io.es_ms.mm_ren, enable)
    val mm_wen = RegEnable(io.es_ms.mm_wen, enable)
    val mm_mask = RegEnable(io.es_ms.mm_mask, enable)
    val mm_unsigned = RegEnable(io.es_ms.mm_unsigned, enable)
    val csr_wen = RegEnable(io.es_ms.csr_wen, enable)
    val csr_addr = RegEnable(io.es_ms.csr_addr, enable)
    val csr_wdata = RegEnable(io.es_ms.csr_wdata, enable)
    val csr_wmask = RegEnable(io.es_ms.csr_wmask, enable)
    val exc = RegEnable(io.es_ms.exc, enable)
    val exc_cause = RegEnable(io.es_ms.exc_cause, enable)
    val mret = RegEnable(io.es_ms.mret, enable)

    io.data_slave.r.ready := true.B
    io.data_slave.b.ready := true.B

    val rdata = RegInit(0.U(64.W))
    val rfire = RegInit(false.B)
    when (ms_allow_in)
    {
        rfire := false.B
    }
    .elsewhen (io.data_slave.r.fire)
    {
        rdata := io.data_slave.r.bits.data
        rfire := true.B
    }

    val bfire = RegInit(false.B)
    when (ms_allow_in)
    {
        bfire := false.B
    }
    .elsewhen (io.data_slave.b.fire)
    {
        bfire := true.B
    }

    val read_data = rfire ? rdata : io.data_slave.r.bits.data
    val mm_rdata = MuxCase(
        0.U(64.W),
        Seq(
            (mm_mask === 0x1.U)  -> Cat(Fill(56, Mux(mm_unsigned, 0.U(1.W), read_data(7))),  read_data(7, 0)),
            (mm_mask === 0x3.U)  -> Cat(Fill(48, Mux(mm_unsigned, 0.U(1.W), read_data(15))), read_data(15, 0)),
            (mm_mask === 0xf.U)  -> Cat(Fill(32, Mux(mm_unsigned, 0.U(1.W), read_data(31))), read_data(31, 0)),
            (mm_mask === 0xff.U) -> read_data
        )
    )

    io.ms_ws.pc := pc

    io.ms_ws.rf_wen := rf_wen
    io.ms_ws.rf_waddr := rf_waddr
    io.ms_ws.rf_wdata := Mux(mm_ren, mm_rdata, alu_result)

    io.ms_ws.csr_wen := csr_wen
    io.ms_ws.csr_addr := csr_addr
    io.ms_ws.csr_wdata := csr_wdata
    io.ms_ws.csr_wmask := csr_wmask
    io.ms_ws.exc := exc
    io.ms_ws.exc_cause := exc_cause
    io.ms_ws.mret := mret
}