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
    val alu_op = Output(Vec(19, UInt(1.W)))
    val alu_src1 = Output(UInt(64.W))
    val alu_src2 = Output(UInt(64.W))
}

class EX_MM extends Bundle
{
    val alu_result = Output(UInt(64.W))
}

class MM_WB extends Bundle
{
    val alu_result = Output(UInt(64.W))
}

class WB_top extends Bundle
{

}

class top extends Module
{
    val io = IO(new Bundle
    {
        val top_IF = new top_IF
        val WB_top = new WB_top

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
    io.top_IF <> IF.io.top_IF
    IF.io.IF_ID  <> ID.io.IF_ID
    ID.io.ID_EX  <> EX.io.ID_EX
    EX.io.EX_MM  <> MM.io.EX_MM
    MM.io.MM_WB  <> WB.io.MM_WB
    WB.io.WB_top <> io.WB_top
    
    val rf = Module(new regfile)
    rf.io.reg_r <> ID.io.reg_r
    rf.io.reg_w <> WB.io.reg_w
    
    io.waddr_reg := rf.io.reg_w.waddr
    io.wdata_reg := rf.io.reg_w.wdata
}
