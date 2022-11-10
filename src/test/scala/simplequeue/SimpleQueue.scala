package simplequeue

import chisel3._
import chiseltest._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.{DataMirror, Direction}

import scala.collection.mutable
import scala.util.control.Breaks.break

/*
class Transaction(width: Int) extends Bundle { 
   val bits: UInt = UInt(width.W) 
}
*/

class MonTx[T <: Data](gen: T) extends Bundle {
  val bits: T = gen
  val cycleStamp = UInt(32.W)}

class DrvTx[T <: Data](gen: T) extends Bundle {
  val bits: T = gen
  val preDelay = UInt(32.W)
  val postDelay = UInt(32.W)
}


object QueueDriver {
  //a function that can drive a transaction into an enqueuing Decoupled interface
  def drive[T <: Data](t: DrvTx[T], interface: DecoupledIO[T], clock: Clock): Unit = {
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

object QueueReciever {
  // a function that can get the bits out from
  // an enqueuing Decoupled interface to a transaction.
  def receive[T <: Data](interface: DecoupledIO[T], clock: Clock, gen: T): MonTx[T] = {
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
    val t = new MonTx(gen)
    val ret = t.Lit(b => b.bits -> peeked)
    println(ret)
    ret
  }
}

object QueueMonitor {
  def monitor[T <: Data](intf: DecoupledIO[T], clock: Clock, gen:T): LazyList[MonTx[T]] = { //what should be the type here: : mutable
    //val recvTxns = mutable.ListBuffer[MonTx[T]]()
    def receiveOne(): MonTx[T] = {
      var txn: MonTx[T] = null
      while (true) {
        if (intf.valid.peek().litToBoolean && intf.ready.peek().litToBoolean) {
          val t = new MonTx(gen)
          val recvTxn = t.Lit(_.bits -> intf.bits.peek(), _.cycleStamp -> 0.U)
          txn = recvTxn
        }
        //cycleCount += 1
        clock.step()
        break
      }
      txn
    }
    LazyList.iterate(receiveOne())(_ => receiveOne())
    // LazyList(new Iterator[MonTx[T]] {
    //   override def hasNext: Boolean = true

    //   override def next(): MonTx[T] = receiveOne()
    // })
    //var cycleCount = 0;
  }
  

}
