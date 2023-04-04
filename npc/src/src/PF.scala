import chisel3._
import chisel3.util._

class PF extends Module
{
    val io = IO(new Bundle
    {
        val pf_fs = new PF_FS

        val inst_axi = new AXI_Lite_Master

        val pf_ready = Output(Bool())
        val ready = Input(Bool())
    })

    val pc = RegInit(0x7ffffffc.U(64.W))
    val next_pc = Mux(io.pf_fs.br_taken, io.pf_fs.br_target, pc + 4.U)

    when (io.pf_ready)
    {
        pc := next_pc
    }

    io.inst_axi.ar.valid := io.ready
    io.inst_axi.ar.bits.addr := next_pc
    io.inst_axi.ar.bits.prot := 0.U(3.W)
    io.inst_axi.aw.valid := false.B
    io.inst_axi.aw.bits.addr := 0.U(64.W)
    io.inst_axi.aw.bits.prot := 0.U(3.W)
    io.inst_axi.w.valid := false.B
    io.inst_axi.w.bits.data := 0.U(64.W)
    io.inst_axi.w.bits.strb := 0.U(8.W)

    io.pf_ready = io.inst_axi.ar.fire
}