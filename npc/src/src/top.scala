import chisel3._
import chisel3.util._

class top extends Module
{
    val io = IO(new Bundle
    {
        val inst = Input(UInt(64.W))
        val pc = Output(UInt(64.W))
    })
    
    val pc = RegNext(pc + 4, 0x80000000.U(64.W))
    
    val decoder7128 = Module(new decoder(7, 128))
    val decoder38 = Module(new decoder(3, 8))
    decoder7128.io.in := inst(6, 0)
    decoder38.io.in := inst(14, 12)
    
    val inst_addi = decoder7128.io.out(0x13) & decoder_3_8.io.out(0x0)
    
    val rf = Module(new regfile)
    val rs1 = inst(19, 15)
    val rs2 = inst(24, 20)
    val rd = inst(11, 7)
    rf.io.raddr1 := rs1
    rf.io.raddr2 := rs2
    rf.io.waddr := rd
    
    val alu = Module(new alu)
    alu.io.aluOp := decoder38.io.out
    alu.io.aluSrc1 := rf.io.rdata1
    alu.io.aluSrc2 := rf.io.rdata2
    rf.io.wdata := alu.io.aluResult
}
