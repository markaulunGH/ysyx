import chisel3._
import chisel3.util._

class WaddrChannel extends Bundle
{
    val addr = Output(UInt(64.W))
    val prot = Output(UInt(3.W))
}

class WdataChannel extends Bundle
{
    val data = Output(UInt(64.W))
    val strb = Output(UInt(8.W))
}

class WrespChannel extends Bundle
{
    val resp = Output(UInt(2.W))
}

class RaddrChannel extends Bundle
{
    val addr = Output(UInt(64.W))
    val prot = Output(UInt(3.W))
}

class RdataChannel extends Bundle
{
    val data = Output(UInt(64.W))
    val resp = Output(UInt(2.W))
}

class AXI_Lite_Master extends Bundle
{
    val aw = Decoupled(new WaddrChannel)
    val w  = Decoupled(new WdataChannel)
    val ar = Decoupled(new RaddrChannel)
}

class AXI_Lite_Slave extends Bundle
{
    val b = Flipped(Decoupled(new WrespChannel))
    val r = Flipped(Decoupled(new RdataChannel))
}

class PF_FS extends Bundle
{
    val pc = Output(UInt(64.W))
}

class PF_DS extends Bundle
{
    val br_taken = Input(Bool())
    val br_target = Input(UInt(64.W))
}

class FS_DS extends Bundle
{
    val inst = Output(UInt(32.W))
    val pc = Output(UInt(64.W))
}

class DS_ES extends Bundle
{
    val pc = Output(UInt(64.W))
    val alu_in = Flipped(new Alu_in)
    val inst_word = Output(Bool())
    val rf_wen = Output(Bool())
    val rf_waddr = Output(UInt(5.W))
    val mm_ren = Output(Bool())
    val mm_wen = Output(Bool())
    val mm_wdata = Output(UInt(64.W))
    val mm_mask = Output(UInt(8.W))
    val mm_unsigned = Output(Bool())
    val csr_wen = Output(Bool())
    val csr_addr = Output(UInt(12.W))
    val csr_wmask = Output(UInt(64.W))
    val csr_wdata = Output(UInt(64.W))
    val exc = Output(Bool())
    val exc_cause = Output(UInt(64.W))
    val mret = Output(Bool())
}

class ES_MS extends Bundle
{
    val pc = Output(UInt(64.W))
    val alu_result = Output(UInt(64.W))
    val rf_wen = Output(Bool())
    val rf_waddr = Output(UInt(5.W))
    val mm_ren = Output(Bool())
    val mm_wen = Output(Bool())
    val mm_mask = Output(UInt(8.W))
    val mm_unsigned = Output(Bool())
    val csr_wen = Output(Bool())
    val csr_addr = Output(UInt(64.W))
    val csr_wmask = Output(UInt(64.W))
    val csr_wdata = Output(UInt(64.W))
    val exc = Output(Bool())
    val exc_cause = Output(UInt(64.W))
    val mret = Output(Bool())
}

class MS_WS extends Bundle
{
    val pc = Output(UInt(64.W))
    val rf_wen = Output(Bool())
    val rf_waddr = Output(UInt(5.W))
    val rf_wdata = Output(UInt(64.W))
    val csr_wen = Output(Bool())
    val csr_addr = Output(UInt(64.W))
    val csr_wmask = Output(UInt(64.W))
    val csr_wdata = Output(UInt(64.W))
    val exc = Output(Bool())
    val exc_cause = Output(UInt(64.W))
    val mret = Output(Bool())
}

class Top extends Module
{
    val io = IO(new Bundle
    {
        val mm_ren = Output(Bool())
        val mm_raddr = Output(UInt(64.W))
        val mm_rdata = Input(UInt(64.W))
        val mm_wen = Output(Bool())
        val mm_waddr = Output(UInt(64.W))
        val mm_wdata = Output(UInt(64.W))
        val mm_mask = Output(UInt(8.W))

        val pc = Output(UInt(64.W))
        val inst = Output(UInt(32.W))
        val ebreak = Output(Bool())
        val rf = Output(Vec(32, UInt(64.W)))
        val rf_wen = Output(Bool())
        val ready = Output(Bool())
    })
    
    val pf = Module(new PF)
    val fs = Module(new FS)
    val ds = Module(new DS)
    val es = Module(new ES)
    val ms = Module(new MS)
    val ws = Module(new WS)
    pf.io.pf_fs <> fs.io.pf_fs
    pf.io.pf_ds <> ds.io.pf_ds
    fs.io.fs_ds <> ds.io.fs_ds
    ds.io.ds_es <> es.io.ds_es
    es.io.es_ms <> ms.io.es_ms
    ms.io.ms_ws <> ws.io.ms_ws
    val ready = fs.io.fs_ready && ds.io.ds_ready && es.io.es_ready && ms.io.ms_ready && ws.io.ws_ready
    pf.io.ready := ready
    fs.io.ready := ready
    ds.io.ready := ready
    es.io.ready := ready
    ms.io.ready := ready
    ws.io.ready := ready

    val arbiter = Module(new AXI_Arbiter)
    arbiter.io.inst_master <> pf.io.inst_master
    arbiter.io.inst_slave  <> fs.io.inst_slave
    arbiter.io.data_master <> es.io.data_master
    arbiter.io.data_slave  <> ms.io.data_slave

    val sram = Module(new SRAM)
    sram.io.master <> arbiter.io.master
    sram.io.slave  <> arbiter.io.slave
    io.mm_ren := sram.io.mm_ren
    io.mm_raddr := sram.io.mm_raddr
    sram.io.mm_rdata := io.mm_rdata
    io.mm_wen := sram.io.mm_wen
    io.mm_waddr := sram.io.mm_waddr
    io.mm_wdata := sram.io.mm_wdata
    io.mm_mask := sram.io.mm_mask
    
    val rf = Module(new Regfile)
    rf.io.reg_r <> ds.io.reg_r
    rf.io.reg_w <> ws.io.reg_w

    val csr = Module(new Csr)
    csr.io.csr_pc <> ds.io.csr_pc
    csr.io.csr_rw <> ws.io.csr_rw
    
    io.pc := pf.io.pc
    io.inst := fs.io.inst
    io.ebreak := ds.io.ebreak
    io.rf := rf.io.rf
    io.rf_wen := rf.io.rf_wen
    io.ready := ready
}
