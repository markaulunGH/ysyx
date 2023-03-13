import chisel3._
import chisel3.util._

class WS extends Module
{
    val io = IO(new Bundle
    {
        val ms_ws = Flipped(new MS_WS)

        val reg_w = Flipped(new reg_w)
    })

    io.reg_w.wen := io.ms_ws.wen
    io.reg_w.waddr := io.ms_ws.waddr
    io.reg_w.wdata := io.ms_ws.alu_result
}