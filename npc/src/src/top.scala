import chisel3._
import chisel3.util._

class top extends Module
{
    val keyboard_io = IO(new keyboardIO)
    val seg_io = IO(new segIO)
}