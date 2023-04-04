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

    io.axi.Bits.aw.addr := Mux(ms_req, io.ms.Bits.aw.addr, io.fs.Bits.aw.addr)
    io.axi.Bits.aw.prot := Mux(ms_req, io.ms.Bits.aw.prot, io.fs.Bits.aw.prot)
    io.axi.Bits.w.data  := Mux(ms_req, io.ms.Bits.w.data,  io.fs.Bits.w.data)
    io.axi.Bits.w.strb  := Mux(ms_req, io.ms.Bits.w.strb,  io.fs.Bits.w.strb)
    io.axi.Bits.b.resp  := Mux(ms_req, io.ms.Bits.b.resp,  io.fs.Bits.b.resp)
    io.axi.Bits.ar.addr := Mux(ms_req, io.ms.Bits.ar.addr, io.fs.Bits.ar.addr)
    io.axi.Bits.ar.prot := Mux(ms_req, io.ms.Bits.ar.prot, io.fs.Bits.ar.prot)
    io.axi.Bits.r.data  := Mux(ms_req, io.ms.Bits.r.data,  io.fs.Bits.r.data)
    io.axi.Bits.r.resp  := Mux(ms_req, io.ms.Bits.r.resp,  io.fs.Bits.r.resp)
}