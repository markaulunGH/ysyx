import chisel3._
import chisel3.util._

class SRAM extends Module
{
    val io = IO(new Bundle
    {
        val master = Flipped(new AXI_Lite_Master)
        val slave  = Flipped(new AXI_Lite_Slave)
        
        val mm_ren = Output(Bool())
        val mm_raddr = Output(UInt(64.W))
        val mm_rdata = Input(UInt(64.W))
        val mm_wen = Output(Bool())
        val mm_waddr = Output(UInt(64.W))
        val mm_wdata = Output(UInt(64.W))
        val mm_mask = Output(UInt(8.W))
    })

    io.master.aw.ready := true.B
    val awfire = RegInit(false.B)
    val awaddr = RegInit(0.U(64.W))
    when (io.master.aw.fire)
    {
        awfire := true.B
        awaddr := io.master.aw.addr
    }
    .elsewhen (io.master.b.fire)
    {
        awfire := false.B
    }

    io.master.w.ready := true.B
    val wfire = RegInit(false.B)
    val wdata = RegInit(0.U(64.W))
    val wstrb = RegInit(0.U(64.W))
    when (io.master.w.fire)
    {
        wfire := true.B
        wdata := io.master.w.addr
        wstrb := io.master.w.strb
    }
    .elsewhen (io.master.b.fire)
    {
        wfire := false.B
    }

    io.mm_wen := awfire && wfire
    io.mm_waddr := awaddr
    io.mm_wdata := wdata
    io.mm_mask := wstrb
    io.slave.b.valid := awfire && wfire
    io.slave.b.bits.resp := 0.U(2.W)

    io.master.ar.ready := true.B
    val arfire = RegInit(false.B)
    val araddr = RegInit(0.U(64.W))
    when (io.master.ar.fire)
    {
        arfire := true.B
        raddr := io.master.ar.addr
    }
    .elsewhen (io.master.r.fire)
    {
        arfire := false.B
    }

    io.mm_ren := arfire
    io.mm_raddr := raddr
    io.slave.r.valid := arfire
    io.slave.r.bits.data := io.mm_rdata
    io.slave.r.bits.resp := 0.U(2.W)
}