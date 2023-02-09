package simplequeue

import chisel3._
import chiseltest._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.{DataMirror, Direction}

import scala.collection.mutable
import scala.util.control.Breaks.break

class MonTx[T <: Data](gen: T) extends Bundle {
  val bits: T = gen
  val cycleStamp = UInt(32.W)
}

class DrvTx[T <: Data](gen: T) extends Bundle {
  val bits: T = gen
  val preDelay = UInt(32.W)
  val postDelay = UInt(32.W)
}

class SlvTx[T <: Data](gen: T) extends Bundle {
  val bits: T = gen
  val waitTime = UInt(32.W)
}


object QueueDriver {
  //a function that can drive a transaction into an enqueuing Decoupled interface
  def drive[T <: Data](t: DrvTx[T], interface: DecoupledIO[T], clock: Clock): Unit = {
    assert(t.bits.litOption.isDefined)
    
    interface.bits.poke(t.bits)
    var preDelay = t.preDelay.litValue
    var postDelay = t.postDelay.litValue

    while (preDelay > 0) {
      preDelay = preDelay - 1
      clock.step(1)
    }

    interface.valid.poke(true.B)

    while (!interface.ready.peek().litToBoolean) {
      clock.step(1)
    }

    clock.step(1)
    interface.valid.poke(false.B)

    while (postDelay > 0) {
      postDelay = postDelay - 1
      clock.step(1)
    }
  }
}

// object QueueReciever {
//   // a function that can get the bits out from
//   // an enqueuing Decoupled interface to a transaction.
//   def receive[T <: Data](interface: DecoupledIO[T], clock: Clock, gen: T): SlvTx[T] = {

//     var wait = waitTime.litValue

//     while (wait > 0){
//       wait -= 1
//       interface.ready.poke(false.B)
//       clock.step(1)
//     }

//     interface.ready.poke(true.B)
//     while (!interface.valid.peek().litToBoolean) {
//       clock.step(1)
//     }

//     val peeked: T = interface.bits.peek() //here, peeked has two fields: 

//     clock.step(1)
//     interface.ready.poke(false.B)
//     val t = new SlvTx(gen)
//     val ret = t.Lit(b => b.bits -> peeked, _.waitTime -> waitTime)
//     //println(ret)
//     ret
//   }
// }

// object is just a singleton, we cannot instantiate it, but we can only call functions on it. 
object QueueReciever {
  // a function that can get the bits out from
  // an enqueuing Decoupled interface to a transaction.
  def receive[T <: Data](interface: DecoupledIO[T], waitTime: UInt, clock: Clock, gen: T): SlvTx[T] = {

    // val rand = new scala.util.Random
    // var waitTime = rand.nextInt(10)
    //println(waitTime)
    var wait = waitTime.litValue

    while (wait > 0){
      wait -= 1
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
    val t = new SlvTx(gen)
    val ret = t.Lit(b => b.bits -> peeked, _.waitTime -> waitTime)
    //println(ret)
    ret
  }
}


class QueueMonitor {
  var cycleCount = 0

  def receiveOne[T <: Data](intf: DecoupledIO[T], clock: Clock, gen:T): Unit = {  //MonTx[T]
    while (true) {
      if (intf.valid.peek().litToBoolean && intf.ready.peek().litToBoolean) {
        val t = new MonTx(gen)
        val recvTxn = t.Lit(_.bits -> intf.bits.peek(), _.cycleStamp -> (cycleCount).U)
        clock.step()
        return recvTxn
      }
      cycleCount += 1
      clock.step()
    }
  }
}

object FifoSimulation{

  def runFIFOSimulation(masterInterface:DecoupledIO[T], slaveInterface:DecoupledIO[T], 
  masterTxns: Seq[MasterDecoupledTx], gen:T, clock:Clock):   //slaveTxns: Seq[SlaveDecoupledTx],
  (ListBuffer[MonTx[T]], ListBuffer[MonTx[T]]) = {
    val masterMonitorTxns = mutable.ListBuffer[MonTx[UInt]]()
    val slaveMonitorTxns = mutable.ListBuffer[MonTx[UInt]]()

    fork {
      masterTxns.foreach { mt =>
          QueueDriver.drive(mt, masterInterface, clock)
          clock.step(1)
        }
    }.fork{
      while (true) {
        QueueReciever.receive(slaveInterface, 5.U, clock, gen)
      }
    }.join()

    fork.withRegion(Monitor){
        println("IN THE MONITOR")
        val monitor = new QueueMonitor()
        while (true) {
          val p = monitor.receiveOne(masterInterface, clock, gen)
          masterMonitorTxns.addOne(p)
          println(p)
        }
      }

      fork.withRegion(Monitor){
        println("IN THE MONITOR")
        val monitor = new QueueMonitor()
        while (true) {
          val p = monitor.receiveOne(slaveInterface, clock, gen)
          slaveMonitorTxns.addOne(p)
          println(p)
        }
      }
    
    return (masterMonitorTxns, masterMonitorTxns)
  }
  
}

// writing AXI driver and monitors. 
// force the thread in the monitor region to implement after all the data. Enforce ordering;

// object QueueMonitor {
//   def monitor[T <: Data](intf: DecoupledIO[T], clock: Clock, gen:T): LazyList[MonTx[T]] = { //what should be the type here: : mutable
//     val recvTxns = mutable.ListBuffer[MonTx[T]]()
//     def receiveOne(): MonTx[T] = {
//       var txn: MonTx[T] = null
//       val cycleCount = 0 
//       while (true) {
//         if (intf.valid.peek().litToBoolean && intf.ready.peek().litToBoolean) {
//           val t = new MonTx(gen)
//           val recvTxn = t.Lit(_.bits -> intf.bits.peek(), _.cycleStamp -> (cycleCount).U)
//           txn = recvTxn
//           recvTxns += txn;
//         }
//         cycleCount += 1
//         clock.step()
//       }
//       txn
//     }
//   }
// }