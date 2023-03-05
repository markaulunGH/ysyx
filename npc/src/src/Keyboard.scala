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

    when (ready === 1.U)
    {
        when (io.nextdata_n === 0.U)
        {
            r_ptr := r_ptr + 1.U
            when (w_ptr === r_ptr + 1.U)
            {
                ready := 0.U
            }
        }
    }

    when (sampling)
    {
        when (count === 10.U)
        {
            when (buffer(0) === 0.U && io.ps2_data === 1.U && buffer(9, 1).xorR)
            {
                fifo(w_ptr) := buffer(8, 1)
                w_ptr := w_ptr + 1.U
                ready := true.B
                overflow := overflow | (r_ptr === (w_ptr + 1.U))
            }
            count := 0.U
        }
        .otherwise
        {
            buffer(count) := ps2_data
            count := count + 1.U
        }
    }

    io.data := fifo(r_ptr)
}
