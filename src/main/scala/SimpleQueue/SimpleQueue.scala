package SimpleQueue

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.{DataMirror, Direction}
import scala.collection.mutable

class Transaction(width: Int) extends Bundle { 
  assert(width <= 32, "The width of the data should be less than 32 bits for decoupled interface to work, but I don't know how to handle the case when the width of the data is more than 32bits and only pass in 32 bits of data.")
  val bits: UInt = UInt(width.W) 
}

class Queue(t:Transaction, interface: DecoupledIO[UInt]) {

  //a function that can drive a transaction 
  //into an enqueuing Decoupled interface
  def drive(t: Transaction, interface: DecoupledIO[UInt]): Unit = {
    while(true) {

      if (t.bits) { // want to check if t.bits is not none. 
        interface.valid.poke(true.B)
      } else {
        interface.valid.poke(false.B)
      }

      if (interface.valid.peek().litToBoolean && interface.ready.peek().litToBoolean) {
        interface.bits.poke(t.bits)
      } else {
        // do nothing. 
      }
    }
  }

}