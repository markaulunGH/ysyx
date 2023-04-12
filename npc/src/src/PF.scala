import chisel3._
import chisel3.util._

class PF extends Module
{
    val io = IO(new Bundle
    {
        val pf_fs = new PF_FS
        val pf_ds = new PF_DS
        val fs_pf = Flipped(new FS_PF)
        val ds_pf = Flipped(new DS_PF)

        val inst_master = new AXI_Lite_Master
    })

    val arfire = RegInit(false.B)
    val pf_ready = io.inst_master.ar.fire || arfire
    val to_fs_valid = pf_ready
    
    val next_pc = Mux(io.ds_pf.br_taken && io.ds_pf.ds_valid, io.ds_pf.br_target, io.fs_pf.pc + 4.U)

    io.inst_master.ar.valid := !arfire && !reset.asBool()
    io.inst_master.ar.bits.addr := next_pc
    io.inst_master.ar.bits.prot := 0.U(3.W)
    io.inst_master.aw.valid := false.B
    io.inst_master.aw.bits.addr := 0.U(64.W)
    io.inst_master.aw.bits.prot := 0.U(3.W)
    io.inst_master.w.valid := false.B
    io.inst_master.w.bits.data := 0.U(64.W)
    io.inst_master.w.bits.strb := 0.U(8.W)

    when (io.ds_pf.ds_allow_in)
    {
        arfire := false.B
    }
    .elsewhen (io.inst_master.ar.fire)
    {
        arfire := true.B
    }

    io.pf_fs.to_fs_valid := to_fs_valid
    io.pf_fs.next_pc := next_pc

    io.pf_ds.pf_ready := pf_ready
}