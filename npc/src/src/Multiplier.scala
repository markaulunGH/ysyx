import chisel3._
import chisel3.util._

class Multiplier_In extends Bundle
{
    val multiplicand = Input(UInt(64.W))
    val multiplier = Input(UInt(64.W))
    val mulw = Input(Bool())
    val signed = Input(UInt(2.W))
}

class Multiplier_Out extends Bundle
{
    val result_hi = Output(UInt(64.W))
    val result_lo = Output(UInt(64.W))
}

class Base_Multipiler extends Module
{
    val in = IO(new Multiplier_In)
    val out = IO(new Multiplier_Out)
    val io = IO(new Bundle
    {
        val flush = Input(Bool())
    })
}

class Multiplier extends Base_Multipiler
{
    val x = RegInit(0.U(64.W))
    val y = RegInit(0.U(64.W))
    val res = RegInit(0.U(128.W))

    val valid = RegInit(false.B)
    when (in.valid)
    {
        x := in.bits.multiplicand
        y := Cat(in.bits.multiplier, 0.U(1.W))
        res := 0.U(128.W)
        valid := true.B
    }

    when (valid)
    {
        y := y >> 2
        x := x << 2
        res := MuxCase(0.U(64.W), Seq(
            (y(2, 0) === 0.U(3.W)) -> (res),
            (y(2, 0) === 1.U(3.W)) -> (res + in.bits.multiplicand),
            (y(2, 0) === 2.U(3.W)) -> (res + in.bits.multiplicand),
            (y(2, 0) === 3.U(3.W)) -> (res + (in.bits.multiplicand << 1)),
            (y(2, 0) === 4.U(3.W)) -> (res - (in.bits.multiplicand << 1)),
            (y(2, 0) === 5.U(3.W)) -> (res - (in.bits.multiplicand << 2)),
            (y(2, 0) === 6.U(3.W)) -> (res - (in.bits.multiplicand << 2)),
            (y(2, 0) === 7.U(3.W)) -> (res)
        ))
    }
}