import chisel3._
import chisel3.util._

class PF extends Module
{
    val io = IO(new Bundle
    {
        val pf_fs = new PF_FS
        val fs_pf = Flipped(new FS_PF)
        val ds_pf = Flipped(new DS_PF)

        val inst_master = new AXI_Lite_Master
    })

    val pf_ready = io.inst_master.ar.fire || arfire
    val to_fs_valid = pf_ready
    
    val next_pc = Mux(io.ds_pf.br_taken, io.ds_pf.br_target, io.fs_pf.pc + 4.U)

    val arfire = RegInit(false.B)
    io.inst_master.ar.valid := !arfire && !reset.asBool()
    io.inst_master.ar.bits.addr := next_pc
    io.inst_master.ar.bits.prot := 0.U(3.W)
    io.inst_master.aw.valid := false.B
    io.inst_master.aw.bits.addr := 0.U(64.W)
    io.inst_master.aw.bits.prot := 0.U(3.W)
    io.inst_master.w.valid := false.B
    io.inst_master.w.bits.data := 0.U(64.W)
    io.inst_master.w.bits.strb := 0.U(8.W)

    when (io.inst_master.ar.fire && !io.ready)
    {
        arfire := true.B
    }
    .elsewhen (io.ready)
    {
        arfire := false.B
    }

    io.pf_fs.to_fs_valid := to_fs_valid
    io.pf_fs.next_pc := next_pc
}