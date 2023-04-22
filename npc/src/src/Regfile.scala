import chisel3._
import chisel3.util._

class Regfile_R extends Bundle
{
    val raddr1 = Input(UInt(5.W))
    val rdata1 = Output(UInt(64.W))

    val raddr2 = Input(UInt(5.W))
    val rdata2 = Output(UInt(64.W))
}

class Regfile_W extends Bundle
{
    val wen = Input(Bool())
    val waddr = Input(UInt(5.W))
    val wdata = Input(UInt(64.W))
}

class Regfile extends Module
{
    val rf_r = IO(new Regfile_R)
    val rf_w = IO(new Regfile_W)

    val sim = IO(new Bundle
    {
        val rf = Output(Vec(32, UInt(64.W)))
        val rf_wen = Output(Bool())
    })

    val rf = Reg(Vec(32, UInt(64.W)))

    when (rf_w.wen)
    {
        rf(rf_w.waddr) := rf_w.wdata
    }

    rf_r.rdata1 := Mux(rf_r.raddr1 === 0.U, 0.U, rf(rf_r.raddr1))
    rf_r.rdata2 := Mux(rf_r.raddr2 === 0.U, 0.U, rf(rf_r.raddr2))

    sim.rf := rf
    sim.rf_wen := rf_w.wen
}