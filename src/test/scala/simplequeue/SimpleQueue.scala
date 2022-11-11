package simplequeue

import chisel3._
import chiseltest._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.{DataMirror, Direction}
import scala.collection.mutable

/*
class Transaction(width: Int) extends Bundle { 
   val bits: UInt = UInt(width.W) 
}
*/

class Transaction[T <: Data](gen: T) extends Bundle { 
  val bits: T = gen 
  val cycleStamp = UInt(32.W)}



object QueueDriver {
  //a function that can drive a transaction into an enqueuing Decoupled interface
  def drive[T <: Data](t: Transaction[T], interface: DecoupledIO[T], clock: Clock): Unit = {
    assert(t.bits.litOption.isDefined)
    interface.valid.poke(true.B)
    interface.bits.poke(t.bits)

    while (!interface.ready.peek().litToBoolean) {
      clock.step(1)
    }
    clock.step(1)
    interface.valid.poke(false.B)
  }
}

object QueueReciever extends Iterator[T] {
  // a function that can get the bits out from 
  // an enqueuing Decoupled interface to a transaction. 
  def receive[T <: Data](interface: DecoupledIO[T], clock: Clock, gen: T): Transaction[T] = {
    val rand = new scala.util.Random
    var waitTime = rand.nextInt(10)
    println(waitTime)
    while (waitTime > 0){
      waitTime -= 1
      interface.ready.poke(false.B)
      clock.step(1)
    }
    interface.ready.poke(true.B)
    while (!interface.valid.peek().litToBoolean) {
      clock.step(1)
    }
    val peeked: T = interface.bits.peek()
    clock.step(1)
    interface.ready.poke(false.B)
    val t = new Transaction(gen)
    val ret = t.Lit(b => b.bits -> peeked)
    println(ret)
    ret
  }
}

object QueueMonitor extends Iterator[T]{

  def hasNext = ;

  def next[T <: Data](): Transaction[T] = {
    if (hasNext)
  } 
  
  def monitor[T <: Data](driver_interface: DecoupledIO[T], receiver_interface: DecoupledIO[T], clock: Clock, gen:T)= { //what should be the type here: : mutable 
    val recvTxns = mutable.ListBuffer[Transaction[T]]()
    var cycleCount = 0;
    while (true) {
    if (driver_interface.valid.peek().litToBoolean && receiver_interface.ready.peek().litToBoolean) {
      val t = new Transaction(gen);
      val txn = t.Lit(_.bits -> driver_interface.bits.peek(), _.cycleStamp -> cycleCount.U)
      recvTxns.addOne(txn)
    }
    cycleCount += 1
    clock.step()
    }
  }
  

}
