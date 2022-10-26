package simplequeue

import chisel3._
import chiseltest._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.{DataMirror, Direction}
import scala.collection.mutable

class Transaction(width: Int) extends Bundle { 
  assert(width <= 32, "The width of the data should be less than 32 bits for decoupled interface to work, but I don't know how to handle the case when the width of the data is more than 32bits and only pass in 32 bits of data.")
  val bits: UInt = UInt(width.W) 
}

class Transaction[T <: Data](gen: T) extends Bundle { val bits: T = gen }

object QueueDriver {
  //a function that can drive a transaction 
  //into an enqueuing Decoupled interface
  def drive(t: Transaction, interface: DecoupledIO[UInt], clock: Clock): Unit = {
    assert(t.bits.litOption.isDefined)
    interface.valid.poke(true.B)
    interface.bits.poke(t.bits)

    while (!interface.ready.peek().litToBoolean) {
      clock.step(1)
    }
    // at this point ready and valid are both true
    clock.step(1)
    interface.valid.poke(false.B)
    interface.bits.poke(0.U)  //?? Now the queue only accept 1 value?
  }
}

object QueueReciever {
  // a function that can get the bits out from 
  // an enqueuing Decoupled interface to a transaction. 
  def receive(interface: DecoupledIO[UInt], clock: Clock): Transaction = {
    interface.ready.poke(true.B)
    while (!interface.valid.peek().litToBoolean) {
      clock.step(1)
    }
    val peeked = interface.bits.peek()
    clock.step(1)
    interface.ready.poke(false.B)
    val t = new Transaction(32)
    t.Lit(b => b.bits -> peeked)
  }
}