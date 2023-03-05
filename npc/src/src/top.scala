import chisel3._
import chisel3.util._

class top extends Module
{
    val io = IO(new Bundle
    {
        val ps2_clk = Input(UInt(1.W))
        val ps2_data = Input(UInt(1.W))

        val seg0 = Output(UInt(8.W))
        val seg1 = Output(UInt(8.W))
        val seg2 = Output(UInt(8.W))
        val seg3 = Output(UInt(8.W))
        val seg4 = Output(UInt(8.W))
        val seg5 = Output(UInt(8.W))
        val seg6 = Output(UInt(8.W))
        val seg7 = Output(UInt(8.W))
    })

    val keyboard = Module(new Keyboard)
    val seg = Module(new Seg)

    keyboard.ps2_clk := io.ps2_clk
    keyboard.ps2_data := io.ps2_data
    keyboard.nextdata_n := 0.U
    
    seg.data := keyboard.data
    seg.ready := keyboard.ready
    seg.overflow := keyboard.overflow
    io.seg0 := seg.seg0
    io.seg1 := seg.seg1
    io.seg2 := seg.seg2
    io.seg3 := seg.seg3
    io.seg4 := seg.seg4
    io.seg5 := seg.seg5
    io.seg6 := seg.seg6
    io.seg7 := seg.seg7
}
