import chisel3._
import chisel3.util._

class Multiplier_in extends Bundle
{
    val flush = Input(Bool())
    val multiplicand = Input(UInt(64.W))
    val multiplier = Input(UInt(64.W))
    val mulw = Input(Bool())
    val signed = Input(UInt(2.W))
}

class Multiplier_out extends Bundle
{
    val result_hi = Output(UInt(64.W))
    val result_lo = Output(UInt(64.W))
}

class Multiplier extends Bundle
{
    val io = IO(new Bundle
    {
        val in = Decoupled(new Multiplier_in)
        val out = Decoupled(new Multiplier_out)
    })

    // val y = Cat(io.in.multiplier, 0.U(1.W))
    val y = Reg(UInt(65.W))
    when (io.in.valid)
    {
        y := Cat(io.in.bits.multiplier, 0.U(1.W))
    }
}