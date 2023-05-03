import chisel3._
import chisel3.util._

class Divider_In extends Bundle
{
    val dividend = Output(UInt(64.W))
    val divisor = Output(UInt(64.W))
    val signed = Output(Bool())
}

class Divider_Out extends Bundle
{
    val quotient = Output(UInt(64.W))
    val remainder = Output(UInt(64.W))
}

class Base_Divider extends Module
{
    val in = IO(Flipped(Decoupled(new Divider_In)))
    val out = IO(Decoupled(new Divider_Out))
    val io = IO(new Bundle
    {
        val flush = Input(Bool())
    })
}

class Divider extends Base_Divider
{
    val s_idle :: s_calc :: s_finish :: Nil = Enum(3)
    val state = RegInit(s_idle)

    val finish = Wire(Bool())
    state := MuxLookup(state, s_idle, Seq(
        s_idle -> Mux(in.fire && !io.flush, s_calc, s_idle),
        s_calc -> MuxCase(s_calc, Seq(
            io.flush -> s_idle,
            finish -> s_finish
        )),
        s_finish -> Mux(out.fire || io.flush, s_idle, s_finish)
    ))
    in.ready := state === s_idle
    out.valid := state === s_finish

    val x = RegInit(0.U(128.W))
    val y = RegInit(0.U(65.W))
    val sign = RegInit(0.U(2.W))
    val quotient = RegInit(0.U(64.W))
    val cnt = RegInit(0.U(6.W))

    when (state === s_idle && in.fire && !io.flush)
    {
        x := Cat(0.U(64.W), Mux(in.bits.sign && in.bits.dividend(63), ~in.bits.dividend + 1.U(64.W), in.bits.dividend))
        y := Cat(0.U(64.W), Mux(in.bits.sign && in.bits.divisor(63), ~in.bits.divisor + 1.U(64.W), in.bits.divisor))
        sign := Cat(in.bits.dividend(63), in.bits.divisor(63)) & in.bits.signed
        cnt := 0.U(6.W)
    }
    .elsewhen (state === s_calc)
    {
        quotient := quotient << 1 | (y < x(127, 63))
        x := Mux(y < x(127, 63), (x - Cat(y, 0.U(63.W))) << 1, x << 1)
        cnt := cnt + 1.U(6.W)
    }
    finish := cnt === 63.U(6.W)

    out.bits.quotient := Mux(sign.xorR, ~quotient + 1.U(64.W), quotient)
    out.bits.remainder := Mux(sign(1), ~x(127, 64) + 1.U(64.W), x(127, 64))
}