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

    // io.axi.aw.addr := Mux(ms_req, io.ms.aw.addr, io.fs.aw.addr)
    // io.axi.aw.prot := Mux(ms_req, io.ms.aw.prot, io.fs.aw.prot)
    // io.axi.w.data  := Mux(ms_req, io.ms.w.data,  io.fs.w.data)
    // io.axi.w.strb  := Mux(ms_req, io.ms.w.strb,  io.fs.w.strb)
    // io.axi.b.resp  := Mux(ms_req, io.ms.b.resp,  io.fs.b.resp)
    // io.axi.ar.addr := Mux(ms_req, io.ms.ar.addr, io.fs.ar.addr)
    // io.axi.ar.prot := Mux(ms_req, io.ms.ar.prot, io.fs.ar.prot)
    // io.axi.r.data  := Mux(ms_req, io.ms.r.data,  io.fs.r.data)
    io.axi.r.resp  := Mux(ms_req, io.ms.r.resp,  io.fs.r.resp)
}