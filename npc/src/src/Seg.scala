import chisel3._
import chisel3.util._

class Seg extends Module
{
    val io = IO(new Bundle
    {
        val data = Input(UInt(8.W))
        val ready = Input(UInt(1.W))
        val overflow = Input(UInt(1.W))
        val seg0 = Output(UInt(8.W))
        val seg1 = Output(UInt(8.W))
        val seg2 = Output(UInt(8.W))
        val seg3 = Output(UInt(8.W))
        val seg4 = Output(UInt(8.W))
        val seg5 = Output(UInt(8.W))
        val seg6 = Output(UInt(8.W))
        val seg7 = Output(UInt(8.W))
    })

    val seg0 = RegInit(VecInit(Seq.fill(8)(0.U(1.W))))
    // seg0 := data(3, 0) === 
}
