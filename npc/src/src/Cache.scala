import chisel3._
import chisel3.util._

class Base_Cache extends Module
{

}

class Cache extends Base_Cache
{
    val pseudoRandomNumber = LFSR(16)
}