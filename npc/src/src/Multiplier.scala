import chisel3._
import chisel3.util._

class Multiplier_io extends Bundle
{
    val flush = Input(Bool())

    val mulw = Input(Bool())
    val signed = Input(UInt(2.W))
    val multiplicand = Input(UInt(64.W))
    val multiplier = Input(UInt(64.W))

    val result_hi = Output(UInt(64.W))
    val result_lo = Output(UInt(64.W))
}

class Multiplier extends Bundle
{
    val io = Decoupled(new Multiplier_io)


}