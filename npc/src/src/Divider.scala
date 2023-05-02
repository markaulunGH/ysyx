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

    when (state === s_idle && in.fire && !io.flush)
    {

    }
    .elsewhen (state === s_calc)
    {

    }
}