import chisel3._
import chisel3.util._

class IF extends Module
{
    val io = IO(new Bundle
    {
        val top_IF = Flipped(new top_IF)
        val IF_ID = new IF_ID
    })

    val pc = RegInit(0x80000000L.U(64.W))
    pc := pc + 4.U
    
    io.top_IF.pc := pc
    val inst = io.top_IF.inst

    io.IF_ID.inst := io.top_IF.inst
}