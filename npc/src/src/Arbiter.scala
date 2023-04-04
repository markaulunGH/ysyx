import chisel3._
import chisel3.util._

class Arbiter extends Module
{
    val io = IO(new Bundle
    {
        val fs = new AXI_Lite_IO
        val ms = new AXI_Lite_IO
        val axi = new AXI_Lite_IO
    })

    val ms_req = io.ms.ar.valid || io.ms.aw.valid

    io.axi.aw.bits.addr := Mux(ms_req, io.ms.aw.bits.addr, io.fs.aw.bits.addr)
    io.axi.aw.bits.prot := Mux(ms_req, io.ms.aw.bits.prot, io.fs.aw.bits.prot)
    io.axi.w.bits.data  := Mux(ms_req, io.ms.w.bits.data,  io.fs.w.bits.data)
    io.axi.w.bits.strb  := Mux(ms_req, io.ms.w.bits.strb,  io.fs.w.bits.strb)
    io.axi.b.bits.resp  := Mux(ms_req, io.ms.b.bits.resp,  io.fs.b.bits.resp)
    io.axi.ar.bits.addr := Mux(ms_req, io.ms.ar.bits.addr, io.fs.ar.bits.addr)
    io.axi.ar.bits.prot := Mux(ms_req, io.ms.ar.bits.prot, io.fs.ar.bits.prot)
    io.axi.r.bits.data  := Mux(ms_req, io.ms.r.bits.data,  io.fs.r.bits.data)
    io.axi.r.bits.resp  := Mux(ms_req, io.ms.r.bits.resp,  io.fs.r.bits.resp)
}