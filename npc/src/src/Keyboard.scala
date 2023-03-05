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

    val buffer = RegInit(0.U(10.W))
    val fifo = Reg(Vec(8, UInt(8.W)))
    val w_ptr = RegInit(0.U(3.W))
    val r_ptr = RegInit(0.U(3.W))
    val count = RegInit(0.U(4.W))
    
    val ready = RegInit(0.U(1.W))
    val overflow = RegInit(0.U(1.W))

    io.ready := ready
    io.overflow := overflow

    val ps2_clk_sync = Reg(UInt(3.W))
    ps2_clk_sync := Cat(ps2_clk_sync(1, 0), io.ps2_clk)

    val sampling = ps2_clk_sync(2) & ~ps2_clk_sync(1)

    when (ready)
    {
        when (nextdata_n === 0)
        {
            r_ptr := r_ptr + 1
            when (w_ptr === r_ptr + 1)
                ready := 0
        }
    }
}
