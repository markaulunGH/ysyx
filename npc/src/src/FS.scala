import chisel3._
import chisel3.util._

class FS extends Module
{
    val io = IO(new Bundle
    {
        val fs_ds = new FS_DS
        val pf_fs = Flipped(new PF_FS)
        val ds_fs = Flipped(new DS_FS)
        val es_fs = Flipped(new ES_FS)

        val inst_slave = new AXI_Lite_Slave

        val inst = Output(UInt(32.W))
        val pc = Output(UInt(64.W))
    })

    val fs_valid = RegInit(false.B)
    val fs_ready = fs_valid && (io.inst_slave.r.fire || rfire)
    val fs_allow_in = !fs_valid || fs_ready && io.ds_fs.ds_allow_in
    val to_ds_valid = fs_valid && fs_ready
    when (fs_allow_in)
    {
        fs_valid := io.pf_fs.to_fs_valid
    }
    .elsewhen (io.fs_ds.br_taken && io.ds_fs.to_es_valid && io.es_fs.es_allow_in)
    {
        fs_valid := false.B
    }

    val pc = RegInit(0x7ffffffc.U(64.W))
    when (fs_allow_in && io.pf_fs.to_fs_valid)
    {
        pc := io.pf_fs.next_pc
    }

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

    io.inst := Mux(rfire, rdata, io.inst_slave.r.bits.data)
    io.pc := pc
}