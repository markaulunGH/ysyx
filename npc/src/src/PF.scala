import chisel3._
import chisel3.util._

class PF extends Module
{
    val io = IO(new Bundle
    {
        val pf_fs = new PF_FS
        val pf_ds = new PF_DS

        val inst_master = new AXI_Lite_Master

        val pf_ready = Output(Bool())
        val ready = Input(Bool())
        val pc = Output(UInt(64.W))
    })

    val pc = RegInit(0x7ffffffc.U(64.W))
    val next_pc = Mux(io.pf_ds.br_taken, io.pf_ds.br_target, pc + 4.U)
    pc := Mux(io.ready, next_pc, pc)

    val buffer = RegInit(false.B)
    io.inst_master.ar.valid := !buffer && !reset.asBool()
    io.inst_master.ar.bits.addr := next_pc
    io.inst_master.ar.bits.prot := 0.U(3.W)
    io.inst_master.aw.valid := false.B
    io.inst_master.aw.bits.addr := 0.U(64.W)
    io.inst_master.aw.bits.prot := 0.U(3.W)
    io.inst_master.w.valid := false.B
    io.inst_master.w.bits.data := 0.U(64.W)
    io.inst_master.w.bits.strb := 0.U(8.W)

    buffer := MuxCase(buffer, Seq(
        (io.inst_master.ar.fire && !io.ready) -> true.B,
        (io.ready) -> false.B
    ))

    io.pf_fs.pc := pc
    
    io.pf_ready := io.inst_master.ar.fire || buffer
    io.pc := pc
}