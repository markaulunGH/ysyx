import chisel3._
import chisel3.util._

class FS_DS extends Bundle
{
    val inst = Output(UInt(64.W))
    val pc = Output(UInt(64.W))
    val br_taken = Input(Bool())
    val br_target = Input(UInt(64.W))
}

class DS_ES extends Bundle
{
    val alu_in = Flipped(new Alu_in)
    val inst_word = Output(Bool())
    val rf_wen = Output(UInt(1.W))
    val rf_waddr = Output(UInt(5.W))
    val mm_ren = Output(UInt(1.W))
    val mm_wen = Output(UInt(1.W))
    val mm_wdata = Output(UInt(64.W))
    val mm_mask = Output(UInt(8.W))
    val mm_unsigned = Output(Bool())
    val res_from_mem = Output(Bool())
    val csr_wen = Output(Bool())
    val csr_addr = Output(UInt(12.W))
    val csr_wmask = Output(UInt(64.W))
    val csr_wdata = Output(UInt(64.W))
}

class ES_MS extends Bundle
{
    val alu_result = Output(UInt(64.W))
    val rf_wen = Output(UInt(1.W))
    val rf_waddr = Output(UInt(5.W))
    val mm_mask = Output(UInt(8.W))
    val mm_unsigned = Output(Bool())
    val res_from_mem = Output(Bool())
    val csr_wen = Output(Bool())
    val csr_addr = Output(UInt(64.W))
    val csr_wmask = Output(UInt(64.W))
    val csr_wdata = Output(UInt(64.W))
}

class MS_WS extends Bundle
{
    val rf_wen = Output(UInt(1.W))
    val rf_waddr = Output(UInt(5.W))
    val rf_wdata = Output(UInt(64.W))
    val csr_wen = Output(Bool())
    val csr_addr = Output(UInt(64.W))
    val csr_wmask = Output(UInt(64.W))
    val csr_wdata = Output(UInt(64.W))
}

class Top extends Module
{
    val io = IO(new Bundle
    {
        val pc = Output(UInt(64.W))
        val inst = Input(UInt(64.W))

        val mm_ren = Output(UInt(1.W))
        val mm_raddr = Output(UInt(64.W))
        val mm_rdata = Input(UInt(64.W))
        val mm_wen = Output(UInt(1.W))
        val mm_waddr = Output(UInt(64.W))
        val mm_wdata = Output(UInt(64.W))
        val mm_mask = Output(UInt(8.W))

        val ebreak = Output(Bool())
        val rf = Output(Vec(32, UInt(64.W)))
    })
    
    val fs = Module(new FS)
    val ds = Module(new DS)
    val es = Module(new ES)
    val ms = Module(new MS)
    val ws = Module(new WS)
    fs.io.fs_ds <> ds.io.fs_ds
    ds.io.ds_es <> es.io.ds_es
    es.io.es_ms <> ms.io.es_ms
    ms.io.ms_ws <> ws.io.ms_ws
    
    io.pc := fs.io.pc
    fs.io.inst := io.inst
    
    io.mm_ren := es.io.mm_ren
    io.mm_raddr := es.io.mm_raddr
    ms.io.mm_rdata := io.mm_rdata
    io.mm_wen := es.io.mm_wen
    io.mm_waddr := es.io.mm_waddr
    io.mm_wdata := es.io.mm_wdata
    io.mm_mask := es.io.mm_mask

    val rf = Module(new Regfile)
    rf.io.reg_r <> ds.io.reg_r
    rf.io.reg_w <> ws.io.reg_w

    val csr = Module(new Csr)
    csr.io <> ws.io.csr
    
    io.ebreak := ds.io.ebreak
    io.rf := rf.io.rf
}
