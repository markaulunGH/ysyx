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

    io.axi.bits.aw.addr := Mux(ms_req, io.ms.bits.aw.addr, io.fs.bits.aw.addr)
    io.axi.bits.aw.prot := Mux(ms_req, io.ms.bits.aw.prot, io.fs.bits.aw.prot)
    io.axi.bits.w.data  := Mux(ms_req, io.ms.bits.w.data,  io.fs.bits.w.data)
    io.axi.bits.w.strb  := Mux(ms_req, io.ms.bits.w.strb,  io.fs.bits.w.strb)
    io.axi.bits.b.resp  := Mux(ms_req, io.ms.bits.b.resp,  io.fs.bits.b.resp)
    io.axi.bits.ar.addr := Mux(ms_req, io.ms.bits.ar.addr, io.fs.bits.ar.addr)
    io.axi.bits.ar.prot := Mux(ms_req, io.ms.bits.ar.prot, io.fs.bits.ar.prot)
    io.axi.bits.r.data  := Mux(ms_req, io.ms.bits.r.data,  io.fs.bits.r.data)
    io.axi.bits.r.resp  := Mux(ms_req, io.ms.bits.r.resp,  io.fs.bits.r.resp)
}