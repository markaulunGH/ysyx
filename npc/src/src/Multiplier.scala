import chisel3._
import chisel3.util._

class Multiplier_In extends Bundle
{
    val multiplicand = Output(UInt(64.W))
    val multiplier = Output(UInt(64.W))
    // val mulw = Output(Bool())
    val signed = Output(UInt(2.W))
}

class Multiplier_Out extends Bundle
{
    val result_hi = Output(UInt(64.W))
    val result_lo = Output(UInt(64.W))
}

class Base_Multipiler extends Module
{
    val in = IO(Flipped(Decoupled(new Multiplier_In)))
    val out = IO(Decoupled(new Multiplier_Out))
    val io = IO(new Bundle
    {
        val flush = Input(Bool())
    })
}

class Multiplier extends Base_Multipiler
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
    val y = RegInit(0.U(67.W))
    val res = RegInit(0.U(128.W))
    val cnt = RegInit(0.U(6.W))

    when (state === s_idle && in.fire && !io.flush)
    {
        // x := Mux(in.bits.mulw,
        //     Cat(Fill(96, in.bits.multiplicand(31) & in.bits.signed(1)), in.bits.multiplicand(31, 0)),
        //     Cat(Fill(64, in.bits.multiplicand(63) & in.bits.signed(1)), in.bits.multiplicand)
        // )
        x := Cat(Fill(64, in.bits.multiplicand(63) & in.bits.signed(1)), in.bits.multiplicand)
        // y := Mux(in.bits.mulw,
        //     Cat(Fill(34, in.bits.multiplier(31) & in.bits.signed(0)), in.bits.multiplier(31, 0), 0.U(1.W)),
        //     Cat(Fill(2, in.bits.multiplier(63) & in.bits.signed(0)), in.bits.multiplier, 0.U(1.W))
        // )
        y := Cat(Fill(2, in.bits.multiplier(63) & in.bits.signed(0)), in.bits.multiplier, 0.U(1.W))
        res := 0.U(128.W)
        cnt := 0.U(6.W)
    }
    .elsewhen (state === s_calc)
    {
        y := y >> 2
        x := x << 2
        res := MuxCase(0.U(64.W), Seq(
            (y(2, 0) === 0.U(3.W)) -> (res),
            (y(2, 0) === 1.U(3.W)) -> (res + x),
            (y(2, 0) === 2.U(3.W)) -> (res + x),
            (y(2, 0) === 3.U(3.W)) -> (res + (x << 1)),
            (y(2, 0) === 4.U(3.W)) -> (res - (x << 1)),
            (y(2, 0) === 5.U(3.W)) -> (res - x),
            (y(2, 0) === 6.U(3.W)) -> (res - x),
            (y(2, 0) === 7.U(3.W)) -> (res)
        ))
        cnt := cnt + 1.U
    }
    // finish := cnt === Mux(in.bits.mulw, 16.U(6.W), 32.U(6.W))
    finish := cnt === 32.U(6.W)

    out.bits.result_hi := res(127, 64)
    out.bits.result_lo := res(63, 0)
}