import chisel3._
import chisel3.util._

class Reg_R extends Bundle
{
    val raddr1 = Input(UInt(5.W))
    val rdata1 = Output(UInt(64.W))

    val raddr2 = Input(UInt(5.W))
    val rdata2 = Output(UInt(64.W))
}

class Reg_W extends Bundle
{
    val wen = Input(Bool())
    val waddr = Input(UInt(5.W))
    val wdata = Input(UInt(64.W))
}

class Regfile extends Module
{
    val reg_r = IO(new Reg_R)
    val reg_w = IO(new Reg_W)

    val sim = IO(new Bundle
    {
        val rf = Output(Vec(32, UInt(64.W)))
        val rf_wen = Output(Bool())
    })

    val rf = Reg(Vec(32, UInt(64.W)))

    when (reg_w.wen)
    {
        rf(reg_w.waddr) := reg_w.wdata
    }

    reg_r.rdata1 := Mux(reg_r.raddr1 === 0.U, 0.U, rf(reg_r.raddr1))
    reg_r.rdata2 := Mux(reg_r.raddr2 === 0.U, 0.U, rf(reg_r.raddr2))

    sim.rf := rf
    sim.rf_wen := reg_w.wen
}