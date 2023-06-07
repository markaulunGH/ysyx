import chisel3._
import chisel3.util._
import chisel3.util.random.LFSR64

class Base_Cache extends Module
{

}

class Cache extends Base_Cache
{
    val pseudoRandomNumber = LFSR64()
}