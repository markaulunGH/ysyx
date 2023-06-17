import chisel3._
import chisel3.util._

class AXI_Arbiter extends Module
{
    val inst_master  = IO(Flipped(new AXI_Lite_Master))
    val inst_slave   = IO(Flipped(new AXI_Lite_Slave))
    val data_master  = IO(Flipped(new AXI_Lite_Master))
    val data_slave   = IO(Flipped(new AXI_Lite_Slave))
    val master       = IO(new AXI_Lite_Master)
    val slave        = IO(new AXI_Lite_Slave)

    val widle = RegInit(true.B)
    val awaddr = RegInit(0.U(64.W))
    when (master.aw.valid) {
        widle := false.B
        awaddr := master.aw.bits.addr
    } .elsewhen (slave.b.fire) {
        widle := true.B
    }
    
    val ridle = RegInit(true.B)
    when (master.ar.valid) {
        ridle := false.B
    } .elsewhen (slave.r.fire) {
        ridle := true.B
    }

    val data_req = RegInit(false.B)
    when (data_master.ar.fire) {
        data_req := true.B
    } .elsewhen (data_slave.r.fire) {
        data_req := false.B
    }
    // when (data_slave.r.fire)
    // {
    //     data_req := false.B
    // }
    // .elsewhen (data_master.ar.valid && (ridle || slave.r.fire))
    // {
    //     data_req := true.B
    // }

    master.aw.valid      := data_master.aw.valid
    data_master.aw.ready := master.aw.ready
    inst_master.aw.ready := false.B
    master.aw.bits.addr  := data_master.aw.bits.addr
    master.aw.bits.prot  := data_master.aw.bits.prot

    master.w.valid      := data_master.w.valid
    data_master.w.ready := master.w.ready
    inst_master.w.ready := false.B
    master.w.bits.data  := data_master.w.bits.data
    master.w.bits.strb  := data_master.w.bits.strb

    data_slave.b.valid     := slave.b.valid
    inst_slave.b.valid     := false.B
    slave.b.ready          := data_slave.b.ready
    data_slave.b.bits.resp := slave.b.bits.resp
    inst_slave.b.bits.resp := 0.U(3.W)

    master.ar.valid      := Mux(data_master.ar.valid, data_master.ar.valid, inst_master.ar.valid) && (widle || master.ar.bits.addr =/= awaddr) && (ridle || slave.r.fire)
    data_master.ar.ready := Mux(data_master.ar.valid, master.ar.ready, false.B) && (widle || data_master.ar.bits.addr =/= awaddr) && (ridle || slave.r.fire)
    inst_master.ar.ready := Mux(data_master.ar.valid, false.B, master.ar.ready) && (widle || inst_master.ar.bits.addr =/= awaddr) && (ridle || slave.r.fire)
    master.ar.bits.addr  := Mux(data_master.ar.valid, data_master.ar.bits.addr, inst_master.ar.bits.addr)
    master.ar.bits.prot  := Mux(data_master.ar.valid, data_master.ar.bits.prot, inst_master.ar.bits.prot)

    data_slave.r.valid     := Mux(data_req, slave.r.valid, false.B)
    inst_slave.r.valid     := Mux(data_req, false.B, slave.r.valid)
    slave.r.ready          := Mux(data_req, data_slave.r.ready, inst_slave.r.ready)
    data_slave.r.bits.data := slave.r.bits.data
    inst_slave.r.bits.data := slave.r.bits.data
    data_slave.r.bits.resp := slave.r.bits.resp
    inst_slave.r.bits.resp := slave.r.bits.resp
}