import chisel3._
import chisel3.util._
import chisel3.util.random

class Base_Cache extends Module
{

}

class Cache extends Base_Cache
{
 val randomPosition = LFSR64()(5, 0)
}