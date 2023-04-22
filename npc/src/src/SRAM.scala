import chisel3._
import chisel3.util._

class SRAM extends Module
{
    val master = IO(Flipped(new AXI_Lite_Master))
    val slave  = IO(Flipped(new AXI_Lite_Slave))
        
    val sim = IO(new Bundle
    {
        val mm_ren = Output(Bool())
        val mm_raddr = Output(UInt(64.W))
        val mm_rdata = Input(UInt(64.W))
        val mm_wen = Output(Bool())
        val mm_waddr = Output(UInt(64.W))
        val mm_wdata = Output(UInt(64.W))
        val mm_mask = Output(UInt(8.W))
    })

    master.aw.ready := true.B
    val awfire = RegInit(false.B)
    val awaddr = RegInit(0.U(64.W))
    when (master.aw.fire)
    {
        awfire := true.B
        awaddr := master.aw.bits.addr
    }
    .elsewhen (slave.b.fire)
    {
        awfire := false.B
    }

    master.w.ready := true.B
    val wfire = RegInit(false.B)
    val wdata = RegInit(0.U(64.W))
    val wstrb = RegInit(0.U(64.W))
    when (master.w.fire)
    {
        wfire := true.B
        wdata := master.w.bits.data
        wstrb := master.w.bits.strb
    }
    .elsewhen (slave.b.fire)
    {
        wfire := false.B
    }

    sim.mm_wen := awfire && wfire
    sim.mm_waddr := awaddr
    sim.mm_wdata := wdata
    sim.mm_mask := wstrb
    slave.b.valid := awfire && wfire
    slave.b.bits.resp := 0.U(2.W)

    master.ar.ready := true.B
    val arfire = RegInit(false.B)
    val araddr = RegInit(0.U(64.W))
    when (master.ar.fire)
    {
        arfire := true.B
        araddr := master.ar.bits.addr
    }
    .elsewhen (slave.r.fire)
    {
        arfire := false.B
    }

    sim.mm_ren := arfire
    sim.mm_raddr := araddr
    slave.r.valid := arfire
    slave.r.bits.data := mm_rdata
    slave.r.bits.resp := 0.U(2.W)
}