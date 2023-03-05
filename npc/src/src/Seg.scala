import chisel3._
import chisel3.util._

class hex2seg extends Module
{
    val io = IO(new Bundle
    {
        val num = Input(UInt(4.W))
        val seg = Output(UInt(8.W))
    })

    val lut = List(0x3f, 0x06, 0x5b, 0x4f, 0x66, 0x6d, 0x7d, 0x07, 0x7f, 0x6f, 0x77, 0x7c, 0x39, 0x5e, 0x79, 0x71)

    io.seg := 0.U
    for (i <- 0 until 16)
    {
        when (io.num === i.U)
        {
            io.seg := lut(i).U
        }
    }
}

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

    val seg0 = Module(new hex2seg)
    seg0.io.num := RegEnable(io.data(3, 0), io.ready.asBool)
    io.seg0 := seg0.io.seg.U

    val seg1 = Module(new hex2seg)
    seg1.io.num := RegEnable(io.data(7, 4), io.ready.asBool)
    io.seg1 := seg1.io.seg


}
