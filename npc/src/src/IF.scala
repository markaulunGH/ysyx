import chisel3._
import chisel3.util._

class IF extends Module
{
    val io = IO(new Bundle
    {
        val pc = Output(UInt(64.W))
        val inst = Input(UInt(64.W))
        
        val IF_ID = new IF_ID
    })

    val pc = RegInit(0x80000000L.U(64.W))
    pc := pc + 4.U
    
    io.top_IF.pc := pc
    val inst = io.top_IF.inst

    io.IF_ID.inst := io.top_IF.inst
}