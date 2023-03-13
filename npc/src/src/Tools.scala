import chisel3._
import chisel3.util._

class Decoder(inWidth: Int, outWidth: Bool) extends Module
{
    val io = IO(new Bundle
    {
        val in = Input(UInt(inWidth.W))
        val out = Output(UInt(outWidth.W))
    })

    val out = Wire(Vec(outWidth, UInt(1.W)))
    for (i <- 0 until outWidth)
    {
        out(i) := io.in === i.U
    }
    
    io.out := out.asUInt
}