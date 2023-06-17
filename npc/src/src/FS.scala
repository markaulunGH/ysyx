import chisel3._
import chisel3.util._

class FS_PF extends Bundle
{
    val pc = Output(UInt(64.W))
}

class FS_DS extends Bundle
{
    val to_ds_valid = Output(Bool())
    val inst = Output(UInt(32.W))
    val pc = Output(UInt(64.W))
}

class FS extends Module
{
    val fs_pf = IO(new FS_PF)
    val fs_ds = IO(new FS_DS)
    val pf_fs = IO(Flipped(new PF_FS))
    val ds_fs = IO(Flipped(new DS_FS))
    val es_fs = IO(Flipped(new ES_FS))

    val inst_slave = IO(new AXI_Lite_Slave)

    val rfire = RegInit(false.B)
    
    val fs_valid = RegInit(false.B)
    val fs_ready = fs_valid && (inst_slave.r.fire || rfire)
    val fs_allow_in = dontTouch(!fs_valid || fs_ready && ds_fs.ds_allow_in)
    val to_ds_valid = fs_valid && fs_ready
    when (fs_allow_in)
    {
        fs_valid := pf_fs.to_fs_valid
    }
    .elsewhen (ds_fs.br_taken && ds_fs.to_es_valid && es_fs.es_allow_in)
    {
        fs_valid := false.B
    }

    val pc = RegInit(0x7ffffffc.U(64.W))
    when (fs_allow_in && pf_fs.to_fs_valid)
    {
        pc := pf_fs.next_pc
    }

    val rdata = RegInit(0.U(32.W))

    inst_slave.r.ready := !rfire && !reset.asBool()
    when (fs_allow_in)
    {
        rfire := false.B
    }
    .elsewhen (inst_slave.r.fire)
    {
        rdata := inst_slave.r.bits.data
        rfire := true.B
    }

    inst_slave.b.ready := false.B

    fs_pf.pc := pc

    fs_ds.to_ds_valid := to_ds_valid
    fs_ds.inst := Mux(rfire, rdata, inst_slave.r.bits.data)
    fs_ds.pc := pc
}