import chisel3._
import chisel3.util._

class ES extends Module
{
    val io = IO(new Bundle
    {
        val ds_es = Flipped(new DS_ES)
        val es_mm = new ES_MS
    })
    val alu = Module(new Alu)
    io.ds_es.alu <> alu.io.in

    io.es_mm.alu_result := alu.io.alu_result
    io.es_mm.wen := io.ds_es.wen
    io.es_mm.waddr := io.ds_es.waddr
}