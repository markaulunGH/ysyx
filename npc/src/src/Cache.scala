import chisel3._
import chisel3.util._
import chisel3.util.random._

class Base_Cache extends Module
{

}

class Cache extends Base_Cache
{
    val pseudoRandomNumber = LFSR16()

    val s_idle :: s_lookup :: s_miss :: s_replace :: s_refill :: Nil = Enum(5)

    // state := MuxLookup(state, s_idle, Seq(
    //     s_idle -> 
    // ))
    
    val s_wbidle :: s_wbwrite :: Nil = Enum(2)
}