import chisel3._
import chisel3.util._

class DS extends Module
{
    val io = IO(new Bundle
    {
        val fs_ds = Flipped(new FS_DS)
        val ds_es = new DS_ES

        val reg_r = Flipped(new reg_r)
    })

    val decoder7128 = Module(new Decoder(7, 128))
    val decoder38 = Module(new Decoder(3, 8))
    decoder7128.io.in := io.fs_ds.inst(6, 0)
    decoder38.io.in := io.fs_ds.inst(14, 12)

    val inst_addi = decoder7128.io.out(0x13) & decoder38.io.out(0x0)

    io.reg_r.raddr1 := io.fs_ds.inst(19, 15)
    io.reg_r.raddr2 := io.fs_ds.inst(24, 20)

    for (i <- 0 until 19)
    {
        io.ds_es.alu.alu_op(i) := 0.U
    }
    io.ds_es.alu.alu_op(0) := inst_addi
    io.ds_es.alu.alu_src1 := io.reg_r.rdata1
    io.ds_es.alu.alu_src2 := io.fs_ds.inst(31, 20)

    io.ds_es.wen := inst_addi
    io.ds_es.waddr := io.fs_ds.inst(11, 7)
}