import chisel3._
import chisel3.util._

class top extends Module
{
    val io = IO(new Bundle
    {
        val inst = Input(UInt(64.W))
        val pc = Output(UInt(64.W))

        val result = Output(UInt(64.W))
    })
    
    val pc = RegInit(0x7ffffffc.U(64.W))
    pc := pc + 4.U
    io.pc := pc
    
    // val decoder7128 = Module(new decoder(7, 128))
    // val decoder38 = Module(new decoder(3, 8))
    // decoder7128.io.in := io.inst(6, 0)
    // decoder38.io.in := io.inst(14, 12)
    // val inst_addi = decoder7128.io.out(0x13) & decoder38.io.out(0x0)
    
    val inst_addi = io.inst(6, 0) === 0x13 & io.inst(14, 12) === 0x0
    
    val rf = Module(new regfile)
    val rs1 = io.inst(19, 15)
    val rs2 = io.inst(24, 20)
    val rd = io.inst(11, 7)
    rf.io.raddr1 := rs1
    rf.io.raddr2 := rs2
    rf.io.waddr := rd
    rf.io.wen := inst_addi
    
    val alu = Module(new alu)
    alu.io.aluOp := decoder38.io.out
    alu.io.aluSrc1 := rf.io.rdata1
    alu.io.aluSrc2 := io.inst(31, 20)
    rf.io.wdata := alu.io.aluResult

    io.result := rf.io.wdata
}
