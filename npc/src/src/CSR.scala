import chisel3._
import chisel3.util._

class CSR_RW extends Bundle
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

class CSR_PC extends Bundle
{
    val mtvec = Output(UInt(64.W))
    val mepc = Output(UInt(64.W))
}

class CSR extends Module
{
    val csr_rw = IO(new CSR_RW)
    val csr_pc = IO(new CSR_PC)

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
    when (csr_rw.exc)
    {
        mstatus_mpp := 0x3.U
        mstatus_mpie := mstatus_mie
        mstatus_mie := 0.U
    }
    .elsewhen (csr_rw.mret)
    {
        mstatus_mpp := 0.U
        mstatus_mpie := 0.U
        mstatus_mie := mstatus_mpie
    }
    .elsewhen (csr_rw.wen && csr_rw.addr === 0x300.U)
    {
        mstatus_uie  := csr_rw.wdata(0)
        mstatus_sie  := csr_rw.wdata(1)
        mstatus_mie  := csr_rw.wdata(3)
        mstatus_upie := csr_rw.wdata(4)
        mstatus_spie := csr_rw.wdata(5)
        mstatus_mpie := csr_rw.wdata(7)
        mstatus_spp  := csr_rw.wdata(8)
        mstatus_mpp  := csr_rw.wdata(12, 11)
        mstatus_fs   := csr_rw.wdata(14, 13)
        mstatus_xs   := csr_rw.wdata(16, 15)
        mstatus_mprv := csr_rw.wdata(17)
        mstatus_sum  := csr_rw.wdata(18)
        mstatus_mxr  := csr_rw.wdata(19)
        mstatus_tvm  := csr_rw.wdata(20)
        mstatus_tw   := csr_rw.wdata(21)
        mstatus_tsr  := csr_rw.wdata(22)
        mstatus_uxl  := csr_rw.wdata(33, 32)
        mstatus_sxl  := csr_rw.wdata(35, 34)
        mstatus_sd   := csr_rw.wdata(63)
    }

    val mtvec = RegInit(0.U(64.W))
    when (csr_rw.wen && csr_rw.addr === 0x305.U)
    {
        mtvec := Cat(csr_rw.wdata(63, 2), 0.U(2.W))
    }

    val mepc = RegInit(0.U(64.W))
    when (csr_rw.exc)
    {
        mepc := csr_rw.pc
    }
    .elsewhen (csr_rw.wen && csr_rw.addr === 0x341.U)
    {
        mepc := csr_rw.wdata
    }

    val mcause = RegInit(0.U(64.W))
    when (csr_rw.exc)
    {
        mcause := Cat(0.U(1.W), csr_rw.exc_cause(62, 0))
    }
    .elsewhen (csr_rw.wen && csr_rw.addr === 0x342.U)
    {
        mcause := csr_rw.wdata
    }

    csr_rw.rdata := MuxLookup(csr_rw.addr, 0.U, Seq(
        0x300.U -> Cat(mstatus_sd, 0.U(27.W), mstatus_sxl, mstatus_uxl, 0.U(9.W), mstatus_tsr, mstatus_tw, mstatus_tvm, mstatus_mxr, mstatus_sum, mstatus_mprv, mstatus_xs, mstatus_fs, mstatus_mpp, 0.U(2.W), mstatus_spp, mstatus_mpie, 0.U(1.W), mstatus_spie, mstatus_upie, mstatus_mie, 0.U(1.W), mstatus_sie, mstatus_uie),
        0x305.U -> mtvec,
        0x341.U -> mepc,
        0x342.U -> mcause
    ))
    csr_pc.mtvec := mtvec
    csr_pc.mepc := mepc
}