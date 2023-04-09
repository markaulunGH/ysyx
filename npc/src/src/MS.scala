import chisel3._
import chisel3.util._

class MS extends Module
{
    val io = IO(new Bundle
    {
        val es_ms = Flipped(new ES_MS)
        val ms_ws = new MS_WS

        val data_slave = new AXI_Lite_Slave

        val ms_ready = Output(Bool())
        val ready = Input(Bool())
    })

    io.data_slave.r.ready := true.B
    io.data_slave.b.ready := true.B

    val rdata = RegInit(0.U(64.W))
    val rfire = RegInit(false.B)
    when (io.data_slave.r.fire)
    {
        rdata := io.data_slave.r.bits.data
        rfire := true.B
    }
    .elsewhen (io.ready)
    {
        rfire := false.B
    }

    val bfire = RegInit(false.B)
    when (io.data_slave.b.fire)
    {
        bfire := true.B
    }
    .elsewhen (io.ready)
    {
        bfire := false.B
    }

    val mm_rdata = MuxCase(
        0.U(64.W),
        Seq(
            (io.es_ms.mm_mask === 0x1.U)  -> Cat(Fill(56, Mux(io.es_ms.mm_unsigned, 0.U(1.W), rdata(7))),  rdata(7, 0)),
            (io.es_ms.mm_mask === 0x3.U)  -> Cat(Fill(48, Mux(io.es_ms.mm_unsigned, 0.U(1.W), rdata(15))), rdata(15, 0)),
            (io.es_ms.mm_mask === 0xf.U)  -> Cat(Fill(32, Mux(io.es_ms.mm_unsigned, 0.U(1.W), rdata(31))), rdata(31, 0)),
            (io.es_ms.mm_mask === 0xff.U) -> rdata
        )
    )

    io.ms_ws.pc := io.es_ms.pc

    io.ms_ws.rf_wen := io.es_ms.rf_wen
    io.ms_ws.rf_waddr := io.es_ms.rf_waddr
    io.ms_ws.rf_wdata := Mux(io.es_ms.mm_ren, mm_rdata, io.es_ms.alu_result)

    io.ms_ws.csr_wen := io.es_ms.csr_wen
    io.ms_ws.csr_addr := io.es_ms.csr_addr
    io.ms_ws.csr_wdata := io.es_ms.csr_wdata
    io.ms_ws.csr_wmask := io.es_ms.csr_wmask
    io.ms_ws.exc := io.es_ms.exc
    io.ms_ws.exc_cause := io.es_ms.exc_cause
    io.ms_ws.mret := io.es_ms.mret

    io.ms_ready := (io.es_ms.mm_ren && (io.data_slave.r.fire || rfire)) || (io.es_ms.mm_wen && (io.data_slave.b.fire || bfire)) || (!io.es_ms.mm_ren && !io.es_ms.mm_wen)
}
