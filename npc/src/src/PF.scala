import chisel3._
import chisel3.util._

class PF_FS extends Bundle
{
    val to_fs_valid = Output(Bool())
    val next_pc = Output(UInt(64.W))
}

class PF_DS extends Bundle
{
    val pf_ready = Output(Bool())
}

class PF extends Module
{
    val pf_fs = IO(new PF_FS)
    val pf_ds = IO(new PF_DS)
    val fs_pf = IO(Flipped(new FS_PF))
    val ds_pf = IO(Flipped(new DS_PF))

    val inst_master = IO(new AXI_Lite_Master)

    val arfire = RegInit(false.B)
    val pf_ready = inst_master.ar.fire || arfire
    val to_fs_valid = pf_ready
    
    val next_pc = Mux(ds_pf.br_taken && ds_pf.ds_valid, ds_pf.br_target, fs_pf.pc + 4.U)

    inst_master.ar.valid := !ds_pf.hazard && !arfire && !reset.asBool()
    inst_master.ar.bits.addr := next_pc
    inst_master.ar.bits.prot := 0.U(3.W)
    inst_master.aw.valid := false.B
    inst_master.aw.bits.addr := 0.U(64.W)
    inst_master.aw.bits.prot := 0.U(3.W)
    inst_master.w.valid := false.B
    inst_master.w.bits.data := 0.U(64.W)
    inst_master.w.bits.strb := 0.U(8.W)

    when (ds_pf.ds_allow_in)
    {
        arfire := false.B
    }
    .elsewhen (inst_master.ar.fire)
    {
        arfire := true.B
    }

    pf_fs.to_fs_valid := to_fs_valid
    pf_fs.next_pc := next_pc

    pf_ds.pf_ready := pf_ready
}