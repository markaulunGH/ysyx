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

class Multiplier extends Module
{
    val io = IO(new Bundle
    {
        val in = Decoupled(new Multiplier_in)
        val out = Decoupled(new Multiplier_out)
    })

    val x = RegInit(0.U(64.W))
    val y = RegInit(0.U(64.W))
    val res = RegInit(0.U(128.W))

    val valid = RegInit(false.B)
    when (io.in.valid)
    {
        x := io.in.bits.multiplicand
        y := Cat(io.in.bits.multiplier, 0.U(1.W))
        res := 0.U(128.W)
        valid := true.B
    }

    when (valid)
    {
        y := y >> 2
        x := x << 2
        res := MuxCase(0.U(64.W), Seq(
            (y(2, 0) === 0.U(3.W)) -> (res),
            (y(2, 0) === 1.U(3.W)) -> (res + io.in.bits.multiplicand),
            (y(2, 0) === 2.U(3.W)) -> (res + io.in.bits.multiplicand),
            (y(2, 0) === 3.U(3.W)) -> (res + (io.in.bits.multiplicand << 1)),
            (y(2, 0) === 4.U(3.W)) -> (res - (io.in.bits.multiplicand << 1)),
            (y(2, 0) === 5.U(3.W)) -> (res - (io.in.bits.multiplicand << 2)),
            (y(2, 0) === 6.U(3.W)) -> (res - (io.in.bits.multiplicand << 2)),
            (y(2, 0) === 7.U(3.W)) -> (res)
        ))
    }
}