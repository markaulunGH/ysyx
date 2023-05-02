import chisel3._
import chisel3.util._

class ES_FS extends Bundle
{
    val es_allow_in = Output(Bool())
}

class ES_DS extends Bundle
{
    val es_allow_in = Output(Bool())
    val es_valid = Output(Bool())
    val to_ms_valid = Output(Bool())
    val alu_result = Output(UInt(64.W))
    val rf_waddr = Output(UInt(5.W))
    val rf_wen = Output(Bool())
    val mm_ren = Output(Bool())
    val csr_wen = Output(Bool())
}

class ES_MS extends Bundle
{
    val to_ms_valid = Output(Bool())
    val pc = Output(UInt(64.W))
    val alu_result = Output(UInt(64.W))
    val rf_wen = Output(Bool())
    val rf_waddr = Output(UInt(5.W))
    val mm_ren = Output(Bool())
    val mm_wen = Output(Bool())
    val mm_mask = Output(UInt(8.W))
    val mm_unsigned = Output(Bool())
    val csr_wen = Output(Bool())
    val csr_addr = Output(UInt(64.W))
    val csr_wmask = Output(UInt(64.W))
    val csr_wdata = Output(UInt(64.W))
    val exc = Output(Bool())
    val exc_cause = Output(UInt(64.W))
    val mret = Output(Bool())

    val inst = Output(UInt(32.W))
    val ebreak = Output(Bool())
}

class ES extends Module
{
    val es_fs = IO(new ES_FS)
    val es_ds = IO(new ES_DS)
    val es_ms = IO(new ES_MS)
    val ds_es = IO(Flipped(new DS_ES))
    val ms_es = IO(Flipped(new MS_ES))

    val data_master = IO(new AXI_Lite_Master)

    val mm_ready = Wire(Bool())
    val mul_ready = Wire(Bool())

    val es_valid = RegInit(false.B)
    val es_ready = mm_ready && mul_ready
    val es_allow_in = !es_valid || es_ready && ms_es.ms_allow_in
    val to_ms_valid = es_valid && es_ready
    when (es_allow_in)
    {
        es_valid := ds_es.to_es_valid
    }

    val es_reg = RegEnable(ds_es, ds_es.to_es_valid && es_allow_in)

    val alu = Module(new ALU)
    alu.io.alu_op := es_reg.alu_op
    alu.io.alu_src1 := es_reg.alu_src1
    alu.io.alu_src2 := es_reg.alu_src2

    val multiplier = Module(new Multiplier)

    val mul_idle :: mul_calc :: Nil = Enum(2)
    val mul_state = RegInit(mul_idle)
    val flush = false.B

    mul_state := MuxLookup(mul_state, mul_idle, Seq(
        mul_idle -> Mux(multiplier.in.fire && !flush, mul_calc, mul_idle),
        mul_calc -> Mux(multiplier.out.fire || flush, mul_idle, mul_calc)
    ))

    multiplier.in.valid := mul_state === mul_idle && (es_reg.alu_op(10) || es_reg.alu_op(11) || es_reg.alu_op(12) || es_reg.alu_op(13))
    multiplier.in.bits.multiplicand := es_reg.alu_src1
    multiplier.in.bits.multiplier := es_reg.alu_src2
    multiplier.in.bits.signed := MuxCase(3.U(2.W), Seq(
        (es_reg.alu_op(10) || es_reg.alu_op(11)) -> 3.U(2.W),
        es_reg.alu_op(12) -> 2.U(2.W),
        es_reg.alu_op(13) -> 0.U(2.W)
    ))
    multiplier.out.ready := mul_state === mul_calc && ms_es.ms_allow_in
    multiplier.io.flush := flush

    mul_ready := (!es_reg.alu_op(10) && !es_reg.alu_op(11) && !es_reg.alu_op(12) && !es_reg.alu_op(13)) || multiplier.out.fire

    val result = MuxCase(alu.io.alu_result, Seq(
        (es_reg.alu_op(10)) -> multiplier.out.bits.result_lo,
        (es_reg.alu_op(11) || es_reg.alu_op(12) || es_reg.alu_op(13)) -> multiplier.out.bits.result_hi

    ))
    val alu_result = Mux(es_reg.inst_word, Cat(Fill(32, result(31)), result(31, 0)), result)

    val arfire = RegInit(false.B)
    data_master.ar.valid := es_reg.mm_ren && !arfire && es_valid
    data_master.ar.bits.addr := alu_result
    data_master.ar.bits.prot := 0.U(3.W)
    when (es_allow_in)
    {
        arfire := false.B
    }
    .elsewhen (data_master.ar.fire)
    {
        arfire := true.B
    }

    val awfire = RegInit(false.B)
    data_master.aw.valid := es_reg.mm_wen && !awfire && es_valid
    data_master.aw.bits.addr := alu_result
    data_master.aw.bits.prot := 0.U(3.W)
    when (es_allow_in)
    {
        awfire := false.B
    }
    .elsewhen (data_master.aw.fire)
    {
        awfire := true.B
    }

    val wfire = RegInit(false.B)
    data_master.w.valid := es_reg.mm_wen && !wfire && es_valid
    data_master.w.bits.data := es_reg.mm_wdata
    data_master.w.bits.strb := es_reg.mm_mask
    when (es_allow_in)
    {
        wfire := false.B
    }
    .elsewhen (data_master.w.fire)
    {
        wfire := true.B
    }

    mm_ready := (data_master.ar.fire || arfire) || (data_master.w.fire || wfire) || (!ds_es.mm_ren && !ds_es.mm_wen)

    es_fs.es_allow_in := es_allow_in

    es_ds.es_allow_in := es_allow_in
    es_ds.es_valid := es_valid
    es_ds.to_ms_valid := to_ms_valid
    es_ds.alu_result := alu_result
    es_ds.rf_waddr := es_reg.rf_waddr
    es_ds.rf_wen := es_reg.rf_wen
    es_ds.mm_ren := es_reg.mm_ren
    es_ds.csr_wen := es_reg.csr_wen

    es_ms.to_ms_valid := to_ms_valid
    es_ms.pc := es_reg.pc

    es_ms.alu_result := alu_result
    es_ms.rf_wen := es_reg.rf_wen
    es_ms.rf_waddr := es_reg.rf_waddr
    es_ms.mm_ren := es_reg.mm_ren
    es_ms.mm_wen := es_reg.mm_wen
    es_ms.mm_mask := es_reg.mm_mask
    es_ms.mm_unsigned := es_reg.mm_unsigned

    es_ms.csr_wen := es_reg.csr_wen
    es_ms.csr_addr := es_reg.csr_addr
    es_ms.csr_wmask := es_reg.csr_wmask
    es_ms.csr_wdata := es_reg.csr_wdata
    es_ms.exc := es_reg.exc
    es_ms.exc_cause := es_reg.exc_cause
    es_ms.mret := es_reg.mret

    es_ms.inst := es_reg.inst
    es_ms.ebreak := es_reg.ebreak
}