import chisel3._
import chisel3.util._

class decoder(inWidth: UInt, outWidth: UInt) extends Module
{
    val io = IO(new Bundle
    {
        val in = Input(UInt(inWidth.W))
        val out = Output(UInt(outWidth.W))
    })

    val out = Vec(outWidth, UInt(1.W))
    for (i <- 0 until outWidth)
    {
        out(i) := io.in === i
    }
    
    io.out := out.asUInt
}