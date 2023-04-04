import chisel3._
import chisel3.util._

class FS extends Module
{
    val io = IO(new Bundle
    {
        val pc = Output(UInt(64.W))
        val inst = Input(UInt(64.W))
        
        val fs_ds = new FS_DS

        val ready = Output(Bool())
    })

    val axi = Module(new AXI_Lite)
    axi.io.ar.bits.addr := io.pc
    axi.io.ar.valid := axi.arstate === 0.U

    val pc = RegInit(0x7ffffffc.U(64.W))
    pc := Mux(io.fs_ds.br_taken, io.fs_ds.br_target, pc + 4.U)
    
    io.pc := pc
    io.fs_ds.inst := io.inst
    io.fs_ds.pc := pc
}