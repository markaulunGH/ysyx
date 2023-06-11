import chisel3._
import chisel3.util._
import chisel3.util.random._

class Cache_Sram(width : Int, depth : Int) extends Module
{
    val io = IO(new Bundle
    {
        val Q    = Output(UInt(width.W))
        val cen  = Input(Bool())
        val wen  = Input(Bool())
        val bwen = Input(UInt(width.W))
        val A    = Input(UInt(log2Ceil(depth).W))
        val D    = Input(UInt(width.W))
    })

    val ram = RegInit(Vec(depth, 0.U(width.W)))
    ram(io.A) := RegEnable((io.D & io.bwen) | (ram(io.A) & ~io.bwen), io.cen && io.wen)
    io.Q := RegEnable(ram(io.A), io.cen && !io.wen)
}

class Bank_IO extends Bundle
{
    val Q   = Output(UInt(64.W))
    val cen = Input(Bool())
    val wen = Input(Bool())
    val ben = Input(UInt(64.W))
    val A   = Input(UInt(7.W))
    val D   = Input(UInt(64.W))
}

class Cache_Line extends Module
{
    val io = IO(new Bundle
    {
        val bank0 = new Bank_IO
        val bank1 = new Bank_IO
        val bank2 = new Bank_IO
        val bank3 = new Bank_IO
    })

    val data0 = new Cache_Sram(64, 128)
    val data1 = new Cache_Sram(64, 128)
    val data2 = new Cache_Sram(64, 128)
    val data3 = new Cache_Sram(64, 128)

    data0.io <> io.bank0
    data1.io <> io.bank1
    data2.io <> io.bank2
    data3.io <> io.bank3
}

class Cache_Way extends Bundle
{
    val tag  = new Cache_Sram(52, 128)
    val V    = new Cache_Sram(1, 128)
    val D    = new Cache_Sram(1, 128)
    val data = new Cache_Line
}

class Cache extends Module
{
    val cpu_master = IO(Flipped(new AXI_Lite_Master))
    val cpu_slave  = IO(Flipped(new AXI_Lite_Slave))
    val master     = IO(new AXI_Lite_Master)
    val slave      = IO(new AXI_Lite_Slave)

    val ways = Seq.fill(2)(new Cache_Way)
    val random_bit = LFSR(1)

    val req = new Bundle
    {
        val valid  = cpu_master.ar.valid || cpu_master.aw.valid
        val op     = cpu_master.aw.valid
        val tag    = Mux(op, cpu_master.aw.bits.addr(63, 13), cpu_master.ar.bits.addr(63, 13))
        val index  = Mux(op, cpu_master.aw.bits.addr(12, 5), cpu_master.ar.bits.addr(12, 5))
        val offset = Mux(op, cpu_master.aw.bits.addr(4, 0), cpu_master.ar.bits.addr(4, 0))
    }

    val s_idle :: s_lookup :: s_aw :: s_w :: s_ar :: s_r :: Nil = Enum(5)
    val state = RegInit(s_idle)

    val hazard = Wire(Bool())
    val hit = Wire(Bool())
    val write_finish = Wire(Bool())
    val read_finish = Wire(Bool())
    val cnt = RegInit(0.U(2.W))

    state := MuxLookup(state, s_idle, Seq(
        s_idle   -> Mux(req.valid && !hazard, s_lookup, s_idle),
        s_lookup -> Mux(hit, Mux(req.valid && !hazard, s_lookup, s_idle), s_aw),
        s_aw     -> Mux(master.aw.fire, s_w, s_aw),
        s_w      -> Mux(slave.b.fire, Mux(cnt === 2.U, s_ar, s_aw), s_w),
        s_ar     -> Mux(master.ar.fire, s_r, s_ar),
        s_r      -> Mux(slave.r.fire, Mux(cnt === 2.U, s_idle, s_r), s_r)
    ))

    for (i <- 0 until 2)
    {
        ways(i).tag.io.cen  := req.valid
        ways(i).tag.io.wen  := DontCare
        ways(i).tag.io.bwen := DontCare
        ways(i).tag.io.A    := req.index
        ways(i).tag.io.D    := DontCare

        ways(i).V.io.cen   := req.valid
        ways(i).V.io.wen   := DontCare
        ways(i).V.io.bwen  := DontCare
        ways(i).V.io.A     := req.index
        ways(i).V.io.D     := DontCare

        ways(i).D.io.cen   := req.valid
        ways(i).D.io.wen   := DontCare
        ways(i).D.io.bwen  := DontCare
        ways(i).D.io.A     := req.index
        ways(i).D.io.D     := DontCare

        ways(i).data.bank0.cen := req.valid
    }

    ways(0).tag.io.cen  := req.valid
    ways(0).tag.io.A    := req.index

    ways(0).V.io.cen    := req.valid




    ways(0).D.io.cen    := req.valid

    ways(1).tag.io.cen := req.valid
    ways(1).V.io.cen   := req.valid
    ways(1).D.io.cen   := req.valid
    
    // val hit0 = 
}