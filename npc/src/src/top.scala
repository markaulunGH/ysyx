import chisel3._
import chisel3.util._

// class top extends Module
// {
//     val io = IO(new Bundle
//     {
//         val ps2_clk = Input(UInt(1.W))
//         val ps2_data = Input(UInt(1.W))

//         val seg0 = Output(UInt(8.W))
//         val seg1 = Output(UInt(8.W))
//         val seg2 = Output(UInt(8.W))
//         val seg3 = Output(UInt(8.W))
//         val seg4 = Output(UInt(8.W))
//         val seg5 = Output(UInt(8.W))
//         val seg6 = Output(UInt(8.W))
//         val seg7 = Output(UInt(8.W))
//     })

//     val keyboard = Module(new Keyboard)
//     val seg = Module(new Seg)

//     keyboard.io.ps2_clk := io.ps2_clk
//     keyboard.io.ps2_data := io.ps2_data
//     keyboard.io.nextdata_n := 0.U
    
//     seg.io.data := keyboard.io.data
//     seg.io.ready := keyboard.io.ready
//     seg.io.overflow := keyboard.io.overflow
//     io.seg0 := seg.io.seg0
//     io.seg1 := seg.io.seg1
//     io.seg2 := seg.io.seg2
//     io.seg3 := seg.io.seg3
//     io.seg4 := seg.io.seg4
//     io.seg5 := seg.io.seg5
//     io.seg6 := seg.io.seg6
//     io.seg7 := seg.io.seg7
// }


class top extends  Module {
    val io = IO(new Bundle
    {
        val num = Input(UInt(4.W))
        val seg = Output(UInt(8.W))
    })

    val lut = List(0x3f, 0x06, 0x5b, 0x4f, 0x66, 0x6d, 0x7d, 0x07, 0x7f, 0x6f, 0x77, 0x7c, 0x39, 0x5e, 0x79, 0x71)

    io.seg := 0.U
    for (i <- 0 until 16)
    {
        if (io.num === i.U)
        {
            io.seg := lut(i).U
        }
    }
}