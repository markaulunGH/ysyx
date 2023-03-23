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
    val wen = Input(Bool())
    val waddr = Input(UInt(5.W))
    val wdata = Input(UInt(64.W))
}

class Regfile extends Module
{
    val io = IO(new Bundle
    {
        val reg_r = new reg_r
        val reg_w = new reg_w

        val rf = Output(Vec(32, UInt(64.W)))
    })

    val rf = Reg(Vec(32, UInt(64.W)))

    when (io.reg_w.wen)
    {
        rf(io.reg_w.waddr) := io.reg_w.wdata
    }

    io.reg_r.rdata1 := Mux(io.reg_r.raddr1 === 0.U, 0.U, rf(io.reg_r.raddr1))
    io.reg_r.rdata2 := Mux(io.reg_r.raddr2 === 0.U, 0.U, rf(io.reg_r.raddr2))

    io.rf := rf
}