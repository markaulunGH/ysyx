import chisel3._
import chisel3.util._

class MS extends Module
{
    val io = IO(new Bundle
    {
        val ms_ds = new MS_DS
        val ms_es = new MS_ES
        val ms_ws = new MS_WS
        val es_ms = Flipped(new ES_MS)
        val ws_ms = Flipped(new WS_MS)

        val data_slave = new AXI_Lite_Slave

        val pc = Output(UInt(64.W))
    })

    val rfire = RegInit(false.B)
    val bfire = RegInit(false.B)
    val mm_ren = Wire(Bool())
    val mm_wen = Wire(Bool())

    val ms_valid = RegInit(false.B)
    val ms_ready = (mm_ren && (io.data_slave.r.fire || rfire)) || (mm_wen && (io.data_slave.b.fire || bfire)) || (!mm_ren && !mm_wen)
    val ms_allow_in = !ms_valid || ms_ready && io.ws_ms.ws_allow_in
    val to_ws_valid = ms_valid && ms_ready
    when (ms_allow_in)
    {
        ms_valid := io.es_ms.to_ms_valid
    }

    val ms_reg = RegEnable(io.es_ms, io.es_ms.to_ms_valid && ms_allow_in)
    mm_ren := ms_reg.mm_ren
    mm_wen := ms_reg.mm_wen

    io.data_slave.r.ready := true.B
    io.data_slave.b.ready := true.B

    val rdata = RegInit(0.U(64.W))
    when (ms_allow_in)
    {
        rfire := false.B
    }
    .elsewhen (io.data_slave.r.fire)
    {
        rdata := io.data_slave.r.bits.data
        rfire := true.B
    }

    when (ms_allow_in)
    {
        bfire := false.B
    }
    .elsewhen (io.data_slave.b.fire)
    {
        bfire := true.B
    }

    val read_data = Mux(rfire, rdata, io.data_slave.r.bits.data)
    val mm_rdata = MuxCase(
        0.U(64.W),
        Seq(
            (ms_reg.mm_mask === 0x1.U)  -> Cat(Fill(56, Mux(ms_reg.mm_unsigned, 0.U(1.W), read_data(7))),  read_data(7, 0)),
            (ms_reg.mm_mask === 0x3.U)  -> Cat(Fill(48, Mux(ms_reg.mm_unsigned, 0.U(1.W), read_data(15))), read_data(15, 0)),
            (ms_reg.mm_mask === 0xf.U)  -> Cat(Fill(32, Mux(ms_reg.mm_unsigned, 0.U(1.W), read_data(31))), read_data(31, 0)),
            (ms_reg.mm_mask === 0xff.U) -> read_data
        )
    )

    io.ms_ds.ms_valid := ms_valid
    io.ms_ds.to_ws_valid := to_ws_valid
    io.ms_ds.rf_wen := ms_reg.rf_wen
    io.ms_ds.rf_waddr := ms_reg.rf_waddr
    io.ms_ds.rf_wdata := Mux(mm_ren, mm_rdata, ms_reg.alu_result)
    io.ms_ds.mm_ren := mm_ren
    io.ms_ds.csr_wen := ms_reg.csr_wen

    io.ms_es.ms_allow_in := ms_allow_in

    io.ms_ws.to_ws_valid := to_ws_valid
    io.ms_ws.pc := ms_reg.pc

    io.ms_ws.rf_wen := ms_reg.rf_wen
    io.ms_ws.rf_waddr := ms_reg.rf_waddr
    io.ms_ws.rf_wdata := Mux(mm_ren, mm_rdata, ms_reg.alu_result)

    io.ms_ws.csr_wen := ms_reg.csr_wen
    io.ms_ws.csr_addr := ms_reg.csr_addr
    io.ms_ws.csr_wdata := ms_reg.csr_wdata
    io.ms_ws.csr_wmask := ms_reg.csr_wmask
    io.ms_ws.exc := ms_reg.exc
    io.ms_ws.exc_cause := ms_reg.exc_cause
    io.ms_ws.mret := ms_reg.mret

    io.ms_ws.inst := ms_reg.inst
    io.ms_ws.ebreak := ms_reg.ebreak

    io.pc := ms_reg.pc
}