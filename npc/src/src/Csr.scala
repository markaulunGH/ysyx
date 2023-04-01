import chisel3._
import chisel3.util._

class Csr_rw extends Bundle
{
    val addr = Input(UInt(12.W))
    val rdata = Output(UInt(64.W))
    val wen = Input(Bool())
    val wdata = Input(UInt(64.W))
    val pc = Input(UInt(64.W))
    val exc = Input(Bool())
    val exc_cause = Input(UInt(64.W))
    val mret = Input(Bool())
}

class Csr_pc extends Bundle
{
    val mtvec = Output(UInt(64.W))
    val mepc = Output(UInt(64.W))
}

class Csr extends Module
{
    val io = IO(new Bundle
    {
        val csr_rw = new Csr_rw
        val csr_pc = new Csr_pc
        val mstatus = Output(UInt(64.W))
    })


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
    when (io.csr_rw.exc)
    {
        mstatus_mpp := 0x3.U
        mstatus_mpie := mstatus_mie
        mstatus_mie := 0.U
    }
    .elsewhen (io.csr_rw.mret)
    {
        mstatus_mpp := 0.U
        mstatus_mpie := 0.U
        mstatus_mie := mstatus_mpie
    }
    .elsewhen (io.csr_rw.wen && io.csr_rw.addr === 0x300.U)
    {
        mstatus_uie  := io.csr_rw.wdata(0)
        mstatus_sie  := io.csr_rw.wdata(1)
        mstatus_mie  := io.csr_rw.wdata(3)
        mstatus_upie := io.csr_rw.wdata(4)
        mstatus_spie := io.csr_rw.wdata(5)
        mstatus_mpie := io.csr_rw.wdata(7)
        mstatus_spp  := io.csr_rw.wdata(8)
        mstatus_mpp  := io.csr_rw.wdata(11, 10)
        mstatus_fs   := io.csr_rw.wdata(13, 12)
        mstatus_xs   := io.csr_rw.wdata(15, 14)
        mstatus_mprv := io.csr_rw.wdata(17)
        mstatus_sum  := io.csr_rw.wdata(18)
        mstatus_mxr  := io.csr_rw.wdata(19)
        mstatus_tvm  := io.csr_rw.wdata(20)
        mstatus_tw   := io.csr_rw.wdata(21)
        mstatus_tsr  := io.csr_rw.wdata(22)
        mstatus_uxl  := io.csr_rw.wdata(25, 24)
        mstatus_sxl  := io.csr_rw.wdata(27, 26)
        mstatus_sd   := io.csr_rw.wdata(63)
    }

    val mtvec = RegInit(0.U(64.W))
    when (io.csr_rw.wen && io.csr_rw.addr === 0x305.U)
    {
        mtvec := Cat(io.csr_rw.wdata(63, 2), 0.U(2.W))
    }

    val mepc = RegInit(0.U(64.W))
    when (io.csr_rw.exc)
    {
        mepc := io.csr_rw.pc
    }
    .elsewhen (io.csr_rw.wen && io.csr_rw.addr === 0x341.U)
    {
        mepc := io.csr_rw.wdata
    }

    val mcause = RegInit(0.U(64.W))
    when (io.csr_rw.exc)
    {
        mcause := Cat(0.U(1.W), io.csr_rw.exc_cause(62, 0))
    }
    .elsewhen (io.csr_rw.wen && io.csr_rw.addr === 0x342.U)
    {
        mcause := io.csr_rw.wdata
    }

    io.csr_rw.rdata := MuxLookup(io.csr_rw.addr, 0.U, Seq(
        0x300.U -> Cat(mstatus_sd, 0.U(27.W), mstatus_sxl, mstatus_uxl, 0.U(9.W), mstatus_tsr, mstatus_tw, mstatus_tvm, mstatus_mxr, mstatus_sum, mstatus_mprv, mstatus_xs, mstatus_fs, mstatus_mpp, 0.U(2.W), mstatus_spp, mstatus_mpie, 0.U(1.W), mstatus_spie, mstatus_upie, mstatus_mie, 0.U(1.W), mstatus_sie, mstatus_uie),
        0x305.U -> mtvec,
        0x341.U -> mepc,
        0x342.U -> mcause
    ))
    io.csr_pc.mtvec := mtvec
    io.csr_pc.mepc := mepc

    mstatus := Cat(mstatus_sd, 0.U(27.W), mstatus_sxl, mstatus_uxl, 0.U(9.W), mstatus_tsr, mstatus_tw, mstatus_tvm, mstatus_mxr, mstatus_sum, mstatus_mprv, mstatus_xs, mstatus_fs, mstatus_mpp, 0.U(2.W), mstatus_spp, mstatus_mpie, 0.U(1.W), mstatus_spie, mstatus_upie, mstatus_mie, 0.U(1.W), mstatus_sie, mstatus_uie)

}