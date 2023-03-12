import chisel3._
import chisel3.util._

class reg_r extends Bundle
{
    val raddr1 = Input(UInt(5.W))
    val rdata1 = Output(UInt(64.W))

    val raddr2 = Input(UInt(5.W))
    val rdata2 = Output(UInt(64.W))
}

class reg_w extends Bundle
{
    val wen = Input(UInt(1.W))
    val waddr = Input(UInt(5.W))
    val wdata = Input(UInt(64.W))
}

class regfile extends Module
{
    val io = IO(new Bundle
    {
        val r = new reg_r
        val w = new reg_w
    })

    val rf = Reg(Vec(32, UInt(64.W)))

    when (io.wen === 1.U)
    {
        rf(io.w.waddr) := io.w.wdata
    }

    io.r.rdata1 := Mux(io.r.raddr1 === 0.U, 0.U, rf(io.r.raddr1))
    io.r.rdata2 := Mux(io.r.raddr2 === 0.U, 0.U, rf(io.r.raddr2))
}