import chisel3._
import chisel3.util._

class Csr_io extends Bundle
{
    val addr = Input(UInt(12.W))
    val rdata = Output(UInt(64.W))
    val wen = Input(Bool())
    val wdata = Input(UInt(64.W))
}

class Csr extends Module
{
    val io = IO(new Csr_io)

    val mstatus = RegInit(0xa00001800L.U(64.W))
    val mtvec = RegInit(0.U(64.W))
    val mepc = RegInit(0.U(64.W))
    val mcause = RegInit(0.U(64.W))

    val mstatus_uie  = RegInit(0.U(1.W))
    val mstatus_sie  = RegInit(0.U(1.W))
    val mstatus_mie  = RegInit(0.U(1.W))
    val mstatus_upie = RegInit(0.U(1.W))
    val mstatus_spie = RegInit(0.U(1.W))
    val mstatus_mpie = RegInit(0.U(1.W))
    val mstatus_spp  = RegInit(0.U(1.W))
    val mstatus_mpp  = RegInit(0x3.U(2.W))
    val mstatus_fs   = RegInit(0.U(2.W))
    val mstatus_xs   = RegInit(0.U(2.W))
    val mstatus_mprv = RegInit(0.U(1.W))
    val mstatus_sum  = RegInit(0.U(1.W))
    val mstatus_mxr  = RegInit(0.U(1.W))
    val mstatus_tvm  = RegInit(0.U(1.W))
    val mstatus_tw   = RegInit(0.U(1.W))
    val mstatus_tsr  = RegInit(0.U(1.W))
    val mstatus_uxl  = RegInit(0x2.U(2.W))
    val mstatus_sxl  = RegInit(0x2.U(2.W))
    val mstatus_sd   = RegInit(0.U(1.W))

    when (io.addr === 0x300.U)
    {

        mstatus_uie  := io.wdata(0)
        mstatus_sie  := io.wdata(1)
        mstatus_mie  := io.wdata(3)
        mstatus_upie := io.wdata(4)
        mstatus_spie := io.wdata(5)
        mstatus_mpie := io.wdata(7)
        mstatus_spp  := io.wdata(8)
        mstatus_mpp  := io.wdata(11, 10)
        mstatus_fs   := io.wdata(13, 12)
        mstatus_xs   := io.wdata(15, 14)
        mstatus_mprv := io.wdata(17)
        mstatus_sum  := io.wdata(18)
        mstatus_mxr  := io.wdata(19)
        mstatus_tvm  := io.wdata(20)
        mstatus_tw   := io.wdata(21)
        mstatus_tsr  := io.wdata(22)
        mstatus_uxl  := io.wdata(25, 24)
        mstatus_sxl  := io.wdata(27, 26)
        mstatus_sd   := io.wdata(63)
    }

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

    io.rdata := MuxLookup(io.addr, 0.U)(Seq(
        0x300.U -> Cat(mstatus_sd, 0.U(27.W), mstatus_sxl, mstatus_uxl, 0.U(9.W), mstatus_tsr, mstatus_tw, mstatus_tvm, mstatus_mxr, mstatus_sum, mstatus_mprv, mstatus_xs, mstatus_fs, mstatus_mpp, 0.U(2.W), mstatus_spp, mstatus_mpie, 0.U(1.W), mstatus_spie, mstatus_upie, mstatus_mie, 0.U(1.W), mstatus_sie, mstatus_uie),
        0x305.U -> mtvec,
        0x341.U -> mepc,
        0x342.U -> mcause
    ))
}