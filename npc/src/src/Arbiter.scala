import chisel3._
import chisel3.util._

class Arbiter extends Module
{
    val io = IO(new Bundle
    {
        val pf = new AXI_Lite_IO
        val ms = new AXI_Lite_IO
        val axi = new AXI_Lite_IO
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

    io.axi.aw.valid     := io.ms.aw.valid
    io.axi.aw.bits.addr := io.ms.aw.bits.addr
    io.axi.aw.bits.prot := io.ms.aw.bits.prot
    io.axi.w.valid      := io.ms.w.valid
    io.axi.w.bits.data  := io.ms.w.bits.data
    io.axi.w.bits.strb  := io.ms.w.bits.strb
    io.axi.b.bits.resp  := io.ms.b.bits.resp
    io.axi.ar.valid     := Mux(ms_req, io.ms.ar.valid, io.pf.ar.valid) && (write_finished || io.axi.ar.bits.addr =/= io.axi.aw.bits.addr)
    io.axi.ar.bits.addr := Mux(ms_req, io.ms.ar.bits.addr, io.pf.ar.bits.addr)
    io.axi.ar.bits.prot := Mux(ms_req, io.ms.ar.bits.prot, io.pf.ar.bits.prot)
    io.axi.r.bits.data  := Mux(ms_req, io.ms.r.bits.data,  io.pf.r.bits.data)
    io.axi.r.bits.resp  := Mux(ms_req, io.ms.r.bits.resp,  io.pf.r.bits.resp)
}