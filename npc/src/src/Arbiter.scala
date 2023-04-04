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

    val data_req = RegInit(false.B)

    when (io.data_master.ar.valid)
    {
        data_req := true.B
    }
    .elsewhen (io.data_slave.r.fire)
    {
        data_req := false.B
    }

    val widle = RegInit(true.B)
    when (io.data_master.aw.valid)
    {
        widle := false.B
    }
    .elsewhen (io.data_master.b.fire)
    {
        widle := true.B
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

    io.master.ar.valid     := Mux(data_req, io.ms.ar.valid, io.pf.ar.valid) && (widle || io.axi.ar.bits.addr =/= io.axi.aw.bits.addr)
    io.data_master.ar.ready  := Mux(data_req, io.master.ar.ready, false.B)
    io.inst_master.ar.ready  := Mux(data_req, false.B, io.master.ar.ready)
    io.master.ar.bits.addr := Mux(data_req, io.ms.ar.bits.addr, io.pf.ar.bits.addr)
    io.master.ar.bits.prot := Mux(data_req, io.ms.ar.bits.prot, io.pf.ar.bits.prot)

    io.data_slave.r.valid     := Mux(data_req, io.slave.r.valid, false.B)
    io.inst_slave.r.valid     := Mux(data_req, false.B, io.slave.r.valid)
    io.slave.r.ready        := Mux(data_req, io.data_slave.r.ready, io.inst_slave.r.ready)
    io.data_slave.r.bits.data := io.slave.r.bits.data
    io.inst_slave.r.bits.data := io.slave.r.bits.data
    io.data_slave.r.bits.resp := io.slave.r.bits.resp
    io.inst_slave.r.bits.resp := io.slave.r.bits.resp
}