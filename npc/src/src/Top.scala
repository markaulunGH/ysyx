import chisel3._
import chisel3.util._

class AW_Channel extends Bundle
{
    val addr = Output(UInt(64.W))
    val prot = Output(UInt(3.W))
}

class W_Channel extends Bundle
{
    val data = Output(UInt(64.W))
    val strb = Output(UInt(8.W))
}

class B_Channel extends Bundle
{
    val resp = Output(UInt(2.W))
}

class AR_Channel extends Bundle
{
    val addr = Output(UInt(64.W))
    val prot = Output(UInt(3.W))
}

class R_Channel extends Bundle
{
    val data = Output(UInt(64.W))
    val resp = Output(UInt(2.W))
}

class AXI_Lite_Master extends Bundle
{
    val aw = Decoupled(new AW_Channel)
    val w  = Decoupled(new W_Channel)
    val ar = Decoupled(new AR_Channel)
}

class AXI_Lite_Slave extends Bundle
{
    val b = Flipped(Decoupled(new B_Channel))
    val r = Flipped(Decoupled(new R_Channel))
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

    val icache = Module(new Cache(2))
    val dcache = Module(new Cache(2))
    icache.cpu_master <> pf.inst_master
    icache.cpu_slave  <> fs.inst_slave
    dcache.cpu_master <> es.data_master
    dcache.cpu_slave  <> ms.data_slave
    
    val arbiter = Module(new AXI_Arbiter)
    arbiter.inst_master <> icache.master
    arbiter.inst_slave  <> icache.slave
    arbiter.data_master <> dcache.master
    arbiter.data_slave  <> dcache.slave

    val sram = Module(new SRAM)
    sram.master <> arbiter.master
    sram.slave  <> arbiter.slave
    io.mm_ren := sram.sim.mm_ren
    io.mm_raddr := sram.sim.mm_raddr
    sram.sim.mm_rdata := io.mm_rdata
    io.mm_wen := sram.sim.mm_wen
    io.mm_waddr := sram.sim.mm_waddr
    io.mm_wdata := sram.sim.mm_wdata
    io.mm_mask := sram.sim.mm_mask
    
    val rf = Module(new Regfile)
    rf.rf_r <> ds.rf_r
    rf.rf_w <> ws.rf_w

    val csr = Module(new CSR)
    csr.csr_pc <> ds.csr_pc
    csr.csr_rw <> ws.csr_rw
    
    io.inst_end := ws.sim.inst_end
    io.pc := ws.sim.pc
    io.inst := ws.sim.inst
    io.ebreak := ws.sim.ebreak
    io.rf := rf.sim.rf
    io.rf_wen := rf.sim.rf_wen
    io.mm_pc := ms.sim.pc
}
