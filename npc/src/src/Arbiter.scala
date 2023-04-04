import chisel3._
import chisel3.util._

class Arbiter extends Module
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

    val ms_req = RegInit(false.B)

    when (io.ms.ar.valid)
    {
        ms_req := true.B
    }
    .elsewhen (io.ms.r.fire)
    {
        ms_req := false.B
    }

    val write_finished = RegInit(true.B)
    when (io.ms.aw.valid)
    {
        write_finished := false.B
    }
    .elsewhen (io.ms.b.fire)
    {
        write_finished := true.B
    }

    io.master.aw.valid     := io.data_master.aw.valid
    io.data_master.aw.ready  := io.master.ready
    io.master.aw.bits.addr := io.data_master.aw.bits.addr
    io.master.aw.bits.prot := io.data_master.aw.bits.prot

    io.master.w.valid     := io.data_master.w.valid
    io.data_master.w.ready  := io.master.w.ready
    io.master.w.bits.data := io.data_master.w.bits.data
    io.master.w.bits.strb := io.data_master.w.bits.strb

    io.data_slave.b.valid     := io.slave.b.valid
    io.slave.b.ready        := io.data_slave.b.ready
    io.data_slave.b.bits.resp := io.slave.b.bits.resp

    io.master.ar.valid     := Mux(ms_req, io.ms.ar.valid, io.pf.ar.valid) && (write_finished || io.axi.ar.bits.addr =/= io.axi.aw.bits.addr)
    io.data_master.ar.ready  := Mux(ms_req, io.master.ar.ready, false.B)
    io.inst_master.ar.ready  := Mux(ms_req, false.B, io.master.ar.ready)
    io.master.ar.bits.addr := Mux(ms_req, io.ms.ar.bits.addr, io.pf.ar.bits.addr)
    io.master.ar.bits.prot := Mux(ms_req, io.ms.ar.bits.prot, io.pf.ar.bits.prot)

    io.data_slave.r.valid     := Mux(ms_req, io.slave.r.valid, false.B)
    io.inst_slave.r.valid     := Mux(ms_req, false.B, io.slave.r.valid)
    io.slave.r.ready        := Mux(ms_req, io.data_slave.r.ready, io.inst_slave.r.ready)
    io.data_slave.r.bits.data := io.slave.r.bits.data
    io.inst_slave.r.bits.data := io.slave.r.bits.data
    io.data_slave.r.bits.resp := io.slave.r.bits.resp
    io.inst_slave.r.bits.resp := io.slave.r.bits.resp
}