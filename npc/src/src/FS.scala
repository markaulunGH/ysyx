import chisel3._
import chisel3.util._

class FS extends Module
{
    val io = IO(new Bundle
    {
        val pc = Output(UInt(64.W))
        val inst = Input(UInt(64.W))

        val pf_fs = Flipped(new PF_FS)
        val fs_ds = new FS_DS
    })



    io.fs_ds.inst := io.inst
    io.fs_ds.pc := io.pf_fs.pc
}