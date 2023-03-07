import chisel3._
import chisel3.util._

class alu extends Module
{
    val io = IO(new Bundle
    {
        val aluOp = Input(UInt(8.W))
        val aluSrc1 = Input(UInt(64.W))
        val aluSrc2 = Input(UInt(64.W))
        val aluResult = Output(UInt(64.W))
    })

    io.aluResult := Mux(io.aluOp(0x0), io.aluSrc1 + io.aluSrc2, 0.U)
}