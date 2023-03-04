import chisel3._
import chisel3.util._

class top extends Module
{
    val io = IO(new Bundle
    {
        val led = Output(UInt(16.W))
    })


    val count = RegNext(Mux(count >= 5000000.U, 0.U, count + 1.U), 0.U(32.W))
    val led = RegInit(1.U(16.W))

    when (count === 0.U(32.W))
    {
        led := Cat(led(14, 0), led(15, 15))
    }

    io.led := led
}
