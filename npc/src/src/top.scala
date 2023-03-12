import chisel3._
import chisel3.util._

class top_IF extends Bundle
{
    val pc = Input(UInt(64.W))
    val inst = Output(UInt(64.W))
}

class IF_ID extends Bundle
{
    val inst = Output(UInt(64.W))
}

class ID_EX extends Bundle
{
    val alu = Flipped(new alu_in)
}

class EX_MM extends Bundle
{
    val alu_result = Output(UInt(64.W))
}

class MM_WB extends Bundle
{
    val alu_result = Output(UInt(64.W))
}

class top extends Module
{
    val io = IO(new Bundle
    {
        val pc = Output(UInt(64.W))
        val inst = Input(UInt(64.W))

        val waddr_reg = Output(UInt(5.W))
        val wdata_reg = Output(UInt(64.W))
        val waddr_mem = Output(UInt(64.W))
        val wdata_mem = Output(UInt(64.W))
    })
    
    val IF = Module(new IF)
    val ID = Module(new ID)
    val EX = Module(new EX)
    val MM = Module(new MM)
    val WB = Module(new WB)
    io.pc := IF.io.pc
    IF.io.inst := io.inst
    IF.io.IF_ID  <> ID.io.IF_ID
    ID.io.ID_EX  <> EX.io.ID_EX
    EX.io.EX_MM  <> MM.io.EX_MM
    MM.io.MM_WB  <> WB.io.MM_WB
    
    val rf = Module(new regfile)
    rf.io.reg_r <> ID.io.reg_r
    rf.io.reg_w <> WB.io.reg_w
    
    io.waddr_reg := rf.io.reg_w.waddr
    io.wdata_reg := rf.io.reg_w.wdata
}
