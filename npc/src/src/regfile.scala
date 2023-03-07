import chisel3._
import chisel3.util._

class regfile extends Module
{
    val io = IO(new Bundle
    {
        val raddr1 = Input(UInt(5.W))
        val rdata1 = Output(UInt(64.W))

        val raddr2 = Input(UInt(5.W))
        val rdata2 = Output(UInt(64.W))

        val wen = Input(UInt(1.W))
        val waddr = Input(UInt(5.W))
        val wdata = Input(UInt(64.W))
    })

    val rf = Reg(Vec(32, UInt(64.W)))

    when (io.wen === 1.U)
    {
        rf(io.waddr) := io.wdata
    }

    io.rdata1 := Mux(io.raddr1 === 0.U, 0.U, rf(io.raddr1))
    io.rdata2 := Mux(io.raddr2 === 0.U, 0.U, rf(io.raddr2))
}