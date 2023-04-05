import chisel3._
import chisel3.util._

class FS extends Module
{
    val io = IO(new Bundle
    {
        val pf_fs = Flipped(new PF_FS)
        val fs_ds = new FS_DS

        val inst_slave = new AXI_Lite_Slave

        val fs_ready = Output(Bool())
        val ready = Input(Bool())
        val inst = Output(UInt(32.W))
    })

    val inst = io.inst_slave.r.bits.data
    val inst_buffer = RegInit(0.U(32.W))
    val inst_buffer_valid = RegInit(true.B)

    io.inst_slave.r.ready := !inst_buffer_valid
    when (io.inst_slave.r.fire)
    {
        inst_buffer := inst
    }

    when (io.inst_slave.r.fire && !io.ready)
    {
        inst_buffer_valid := true.B
    }
    .elsewhen (io.ready)
    {
        inst_buffer_valid := false.B
    }

    io.inst_slave.b.ready := true.B

    io.fs_ds.inst := Mux(inst_buffer_valid, inst_buffer, inst)
    io.fs_ds.pc := io.pf_fs.pc

    io.fs_ready := io.inst_slave.r.fire || inst_buffer_valid
    io.inst := Mux(inst_buffer_valid, inst_buffer, inst)
}