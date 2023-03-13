import chisel3._
import chisel3.util._

class Decoder(inWidth: Int, outWidth: Int) extends Module
{
    val io = IO(new Bundle
    {
        val in = Input(UInt(inWidth.W))
        val out = Output(Vec(outWidth, Bool()))
    })

    for (i <- 0 until outWidth)
    {
        io.out(i) := io.in === i.U
    }
}