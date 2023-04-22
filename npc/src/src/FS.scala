import chisel3._
import chisel3.util._

class FS extends Module
{
    val io = IO(new Bundle
    {
        val fs_pf = new FS_PF
        val fs_ds = new FS_DS
        val pf_fs = Flipped(new PF_FS)
        val ds_fs = Flipped(new DS_FS)
        val es_fs = Flipped(new ES_FS)

        val inst_slave = new AXI_Lite_Slave
    })

    val rfire = RegInit(false.B) // pretend that there is an instruction in the buffer before the first instruction is fetched; maybe problematic
    
    val fs_valid = RegInit(false.B)
    val fs_ready = fs_valid && (io.inst_slave.r.fire || rfire)
    val fs_allow_in = !fs_valid || fs_ready && io.ds_fs.ds_allow_in
    val to_ds_valid = fs_valid && fs_ready
    when (fs_allow_in)
    {
        fs_valid := io.pf_fs.to_fs_valid
    }
    .elsewhen (io.ds_fs.br_taken && io.ds_fs.to_es_valid && io.es_fs.es_allow_in)
    {
        fs_valid := false.B
    }

    val pc = RegInit(0x7ffffffc.U(64.W))
    when (fs_allow_in && io.pf_fs.to_fs_valid)
    {
        pc := io.pf_fs.next_pc
    }

    val rdata = RegInit(0.U(32.W))

    io.inst_slave.r.ready := !rfire && !reset.asBool()
    when (fs_allow_in)
    {
        rfire := false.B
    }
    .elsewhen (io.inst_slave.r.fire)
    {
        rdata := io.inst_slave.r.bits.data
        rfire := true.B
    }

    io.inst_slave.b.ready := false.B

    io.fs_pf.pc := pc

    io.fs_ds.to_ds_valid := to_ds_valid
    io.fs_ds.inst := Mux(rfire, rdata, io.inst_slave.r.bits.data)
    io.fs_ds.pc := pc
}