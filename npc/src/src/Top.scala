import chisel3._
import chisel3.util._

class AWChannel extends Bundle
{
    val addr = Output(UInt(64.W))
    val prot = Output(UInt(3.W))
}

class WChannel extends Bundle
{
    val data = Output(UInt(64.W))
    val strb = Output(UInt(8.W))
}

class BChannel extends Bundle
{
    val resp = Output(UInt(2.W))
}

class ARChannel extends Bundle
{
    val addr = Output(UInt(64.W))
    val prot = Output(UInt(3.W))
}

class RChannel extends Bundle
{
    val data = Output(UInt(64.W))
    val resp = Output(UInt(2.W))
}

class AXI_Lite_Master extends Bundle
{
    val aw = Decoupled(new AWChannel)
    val w  = Decoupled(new WChannel)
    val ar = Decoupled(new ARChannel)
}

class AXI_Lite_Slave extends Bundle
{
    val b = Flipped(Decoupled(new BChannel))
    val r = Flipped(Decoupled(new RChannel))
}

class PF_FS extends Bundle
{
    val to_fs_valid = Output(Bool())
    val next_pc = Output(UInt(64.W))
}

class PF_DS extends Bundle
{
    val pf_ready = Output(Bool())
}

class FS_PF extends Bundle
{
    val pc = Output(UInt(64.W))
}

class FS_DS extends Bundle
{
    val to_ds_valid = Output(Bool())
    val inst = Output(UInt(32.W))
    val pc = Output(UInt(64.W))
}

class DS_PF extends Bundle
{
    val ds_valid = Output(Bool())
    val ds_allow_in = Output(Bool())
    val br_taken = Output(Bool())
    val br_target = Output(UInt(64.W))
}

class DS_FS extends Bundle
{
    val ds_allow_in = Output(Bool())
    val to_es_valid = Output(Bool())
    val br_taken = Output(Bool())
}

class DS_ES extends Bundle
{
    val to_es_valid = Output(Bool())
    val pc = Output(UInt(64.W))
    val alu_op = Output(Vec(18, Bool()))
    val alu_src1 = Output(UInt(64.W))
    val alu_src2 = Output(UInt(64.W))
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

    val inst = Output(UInt(32.W))
    val ebreak = Output(Bool())
}

class ES_FS extends Bundle
{
    val es_allow_in = Output(Bool())
}

class ES_DS extends Bundle
{
    val es_allow_in = Output(Bool())
    val es_valid = Output(Bool())
    val to_ms_valid = Output(Bool())
    val alu_result = Output(UInt(64.W))
    val rf_waddr = Output(UInt(5.W))
    val rf_wen = Output(Bool())
    val mm_ren = Output(Bool())
    val csr_wen = Output(Bool())
}

class ES_MS extends Bundle
{
    val to_ms_valid = Output(Bool())
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

    val inst = Output(UInt(32.W))
    val ebreak = Output(Bool())
}

class MS_DS extends Bundle
{
    val ms_valid = Output(Bool())
    val to_ws_valid = Output(Bool())
    val rf_wen = Output(Bool())
    val rf_waddr = Output(UInt(5.W))
    val rf_wdata = Output(UInt(64.W))
    val mm_ren = Output(Bool())
    val csr_wen = Output(Bool())
}

class MS_ES extends Bundle
{
    val ms_allow_in = Output(Bool())
}

class MS_WS extends Bundle
{
    val to_ws_valid = Output(Bool())
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

    val inst = Output(UInt(32.W))
    val ebreak = Output(Bool())
}

class WS_DS extends Bundle
{
    val ws_valid = Output(Bool())
    val rf_wen = Output(Bool())
    val rf_waddr = Output(UInt(5.W))
    val rf_wdata = Output(UInt(64.W))
}

class WS_MS extends Bundle
{
    val ws_allow_in = Output(Bool())
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

        val inst_end = Output(Bool())
        val pc = Output(UInt(64.W))
        val inst = Output(UInt(32.W))
        val ebreak = Output(Bool())
        val rf = Output(Vec(32, UInt(64.W)))
        val rf_wen = Output(Bool())
        val mm_pc = Output(UInt(64.W))
    })
    
    val pf = Module(new PF)
    val fs = Module(new FS)
    val ds = Module(new DS)
    val es = Module(new ES)
    val ms = Module(new MS)
    val ws = Module(new WS)
    pf.pf_fs <> fs.pf_fs
    pf.pf_ds <> ds.pf_ds
    fs.fs_pf <> pf.fs_pf
    fs.fs_ds <> ds.fs_ds
    ds.ds_pf <> pf.ds_pf
    ds.ds_fs <> fs.ds_fs
    ds.ds_es <> es.ds_es
    es.es_fs <> fs.es_fs
    es.es_ds <> ds.es_ds
    es.es_ms <> ms.es_ms
    ms.ms_ds <> ds.ms_ds
    ms.ms_es <> es.ms_es
    ms.ms_ws <> ws.ms_ws
    ws.ws_ds <> ds.ws_ds
    ws.ws_ms <> ms.ws_ms

    val arbiter = Module(new AXI_Arbiter)
    arbiter.io.inst_master <> pf.inst_master
    arbiter.io.inst_slave  <> fs.inst_slave
    arbiter.io.data_master <> es.data_master
    arbiter.io.data_slave  <> ms.data_slave

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
    rf.io.reg_r <> ds.reg_r
    rf.io.reg_w <> ws.reg_w

    val csr = Module(new CSR)
    csr.io.csr_pc <> ds.csr_pc
    csr.io.csr_rw <> ws.csr_rw
    
    io.inst_end := ws.sim.inst_end
    io.pc := ws.sim.pc
    io.inst := ws.sim.inst
    io.ebreak := ws.sim.ebreak
    io.rf := rf.io.rf
    io.rf_wen := rf.io.rf_wen
    io.mm_pc := ms.sim.pc
}
