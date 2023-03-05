import chisel3._
import chisel3.util._

class top extends Module
{
    val io = IO(new Bundle
    {
        val ps2_clk = Input(UInt(1.W))
        val ps2_data = Input(UInt(1.W))
        val nextdata_n = Input(UInt(1.W))
        val data = Output(UInt(8.W))
        val ready = Output(UInt(1.W))
        val overflow = Output(UInt(1.W))
    })

    val ps2_clk_sync = Reg(UInt(3.W))

    ps2_clk_sync := Cat(ps2_clk_sync(1, 0), ps2_clk)

    val sampling = ps2_clk_sync(2) & ~ps2_clk_sync(1)
}
