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

    keyboard.io.ps2_clk := io.ps2_clk
    keyboard.io.ps2_data := io.ps2_data
    keyboard.io.nextdata_n := 0.U
    
    seg.io.data := keyboard.io.data
    seg.io.ready := keyboard.io.ready
    seg.io.overflow := keyboard.io.overflow
    io.seg0 := seg.io.seg0
    io.seg1 := seg.io.seg1
    io.seg2 := seg.io.seg2
    io.seg3 := seg.io.seg3
    io.seg4 := seg.io.seg4
    io.seg5 := seg.io.seg5
    io.seg6 := seg.io.seg6
    io.seg7 := seg.io.seg7
}
