import chisel3._

class top extends Module
{
    val io = IO(new Bundle
    {
        val led = Output(Reg(UInt(16.W)))
    })

    val count = RegInit(0.U(32.W))
    val led = RegInit(1.U(16.W))

    when (count === 0.U(32.W))
    {
        led := Cat(led(14, 0), led(15))
    }

    when (count >= 5000000)
    {
        count := 0.U(32.W)
    }
    .otherwise
    {
        count := count + 1
    }

    io.led := led
}
