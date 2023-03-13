import chisel3._
import chisel3.util._

class FS_DS extends Bundle
{
    val inst = Output(UInt(64.W))
}

class DS_ES extends Bundle
{
    val alu = Flipped(new alu_in)
    val wen = Output(UInt(1.W))
    val waddr = Output(UInt(5.W))
}

class ES_MS extends Bundle
{
    val alu_result = Output(UInt(64.W))
    val wen = Output(UInt(1.W))
    val waddr = Output(UInt(5.W))
}

class MS_WS extends Bundle
{
    val alu_result = Output(UInt(64.W))
    val wen = Output(UInt(1.W))
    val waddr = Output(UInt(5.W))
}

class Top extends Module
{
    val io = IO(new Bundle
    {
        val pc = Output(UInt(64.W))
        val inst = Input(UInt(64.W))

        val wreg_addr = Output(UInt(5.W))
        val wreg_data = Output(UInt(64.W))
        // val wmem_addr = Output(UInt(64.W))
        // val wmem_data = Output(UInt(64.W))
    })
    
    val fs = Module(new FS)
    val ds = Module(new DS)
    val es = Module(new ES)
    val ms = Module(new MS)
    val ws = Module(new WS)
    io.pc := fs.io.pc
    fs.io.inst := io.inst
    fs.io.fs_ds  <> ds.io.fs_ds
    ds.io.ds_es  <> es.io.ds_es
    es.io.es_mm  <> ms.io.es_mm
    ms.io.ms_ws  <> ws.io.ms_ws
    
    val rf = Module(new Regfile)
    rf.io.reg_r <> ds.io.reg_r
    rf.io.reg_w <> ws.io.reg_w
    
    io.wreg_addr := rf.io.reg_w.waddr
    io.wreg_data := rf.io.reg_w.wdata
}
