import chisel3._
import chisel3.util._

class Csrfile extends Module {
    val io = IO(new Bundle
    {
        val addr = Input(UInt(12.W))
        val wdata = Input(UInt(64.W))
        val rdata = Output(UInt(64.W))
        val wen = Input(Bool())
    })

    val mstatus = RegInit(0xa00001800L.U(64.W))
    val mtvec = RegInit(0.U(64.W))
    val mepc = RegInit(0.U(64.W))
    val mcause = RegInit(0.U(64.W))

    when (io.wen && io.addr === 0x300.U)
    {
        mstatus := io.wdata
    }
    when (io.wen && io.addr === 0x305.U)
    {
        mtvec := io.wdata
    }
    when (io.wen && io.addr === 0x341.U)
    {
        mepc := io.wdata
    }
    when (io.wen && io.addr === 0x342.U)
    {
        mcause := io.wdata
    }

    io.rdata := MuxCase(0.U, Array(
        (io.addr === 0x300.U) -> mstatus,
        (io.addr === 0x305.U) -> mtvec,
        (io.addr === 0x341.U) -> mepc,
        (io.addr === 0x342.U) -> mcause
    ))
}