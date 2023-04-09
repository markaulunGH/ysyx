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

    val rdata = RegInit(0.U(32.W))
    val rfire = RegInit(true.B) // pretend that there is an instruction in the buffer before the first instruction is fetched; maybe problematic

    io.inst_slave.r.ready := !rfire && !reset.asBool()
    when (io.inst_slave.r.fire && !io.ready)
    {
        rdata := io.inst_slave.r.bits.data
        rfire := true.B
    }
    .elsewhen (io.ready)
    {
        rfire := false.B
    }

    io.inst_slave.b.ready := false.B

    io.fs_ds.inst := Mux(rfire, rdata, io.inst_slave.r.bits.data)
    io.fs_ds.pc := io.pf_fs.pc

    io.fs_ready := io.inst_slave.r.fire || rfire
    io.inst := Mux(rfire, rdata, io.inst_slave.r.bits.data)
}