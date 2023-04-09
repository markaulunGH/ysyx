import chisel3._
import chisel3.util._

class AXI_Arbiter extends Module
{
    val io = IO(new Bundle
    {
        val inst_master  = Flipped(new AXI_Lite_Master)
        val inst_slave   = Flipped(new AXI_Lite_Slave)
        val data_master  = Flipped(new AXI_Lite_Master)
        val data_slave   = Flipped(new AXI_Lite_Slave)
        val master       = new AXI_Lite_Master
        val slave        = new AXI_Lite_Slave
    })

    val widle = RegNext(true.B, MuxCase(widle, Seq(
        io.master.aw.valid -> false.B,
        io.slave.b.fire    -> true.B
    )))
    
    val ridle = RegInit(true.B)
    ridle := MuxCase(ridle, Seq(
        io.slave.r.fire -> true.B,
        io.master.ar.valid -> false.B
    ))

    val data_req = RegInit(false.B)
    data_req := MuxCase(data_req, Seq(
        (io.data_master.ar.valid && (ridle || io.slave.r.fire)) -> true.B,
        io.data_slave.r.fire -> false.B
    ))

    io.master.aw.valid      := io.data_master.aw.valid
    io.data_master.aw.ready := io.master.aw.ready
    io.inst_master.aw.ready := false.B
    io.master.aw.bits.addr  := io.data_master.aw.bits.addr
    io.master.aw.bits.prot  := io.data_master.aw.bits.prot

    io.master.w.valid      := io.data_master.w.valid
    io.data_master.w.ready := io.master.w.ready
    io.inst_master.w.ready := false.B
    io.master.w.bits.data  := io.data_master.w.bits.data
    io.master.w.bits.strb  := io.data_master.w.bits.strb

    io.data_slave.b.valid     := io.slave.b.valid
    io.inst_slave.b.valid     := false.B
    io.slave.b.ready          := io.data_slave.b.ready
    io.data_slave.b.bits.resp := io.slave.b.bits.resp
    io.inst_slave.b.bits.resp := 0.U(3.W)

    //potential bugs down here; maybe fixed when changing to pipeline
    io.master.ar.valid      := Mux(io.data_master.ar.valid || data_req, io.data_master.ar.valid, io.inst_master.ar.valid) && (widle || io.master.ar.bits.addr =/= io.master.aw.bits.addr) && (ridle || io.slave.r.fire)
    io.data_master.ar.ready := Mux(io.data_master.ar.valid || data_req, io.master.ar.ready, false.B) && (ridle || io.slave.r.fire)
    io.inst_master.ar.ready := Mux(io.data_master.ar.valid || data_req, false.B, io.master.ar.ready) && (ridle || io.slave.r.fire)
    io.master.ar.bits.addr  := Mux(io.data_master.ar.valid || data_req, io.data_master.ar.bits.addr, io.inst_master.ar.bits.addr)
    io.master.ar.bits.prot  := Mux(io.data_master.ar.valid || data_req, io.data_master.ar.bits.prot, io.inst_master.ar.bits.prot)

    io.data_slave.r.valid     := Mux(data_req, io.slave.r.valid, false.B)
    io.inst_slave.r.valid     := Mux(data_req, false.B, io.slave.r.valid)
    io.slave.r.ready          := Mux(data_req, io.data_slave.r.ready, io.inst_slave.r.ready)
    io.data_slave.r.bits.data := io.slave.r.bits.data
    io.inst_slave.r.bits.data := io.slave.r.bits.data
    io.data_slave.r.bits.resp := io.slave.r.bits.resp
    io.inst_slave.r.bits.resp := io.slave.r.bits.resp
}