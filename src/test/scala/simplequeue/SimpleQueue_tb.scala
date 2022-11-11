package simplequeue

import chisel3._
import chiseltest._
import chisel3.util._
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.{TreadleBackendAnnotation, WriteVcdAnnotation}
import chisel3.experimental.BundleLiterals._

import scala.collection.mutable

class QueueTest extends AnyFreeSpec with ChiselScalatestTester {
  "run a test" in {
    val gen = UInt(32.W)
    test(new Queue(gen, 8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.clock.step(10)
      val txns = (0 until 4).map{ i =>
        new DrvTx(gen).Lit(b => b.bits -> (100+i).U, _.preDelay -> 0.U, _.postDelay -> 0.U)
      }
      txns.foreach { t=>
        QueueDriver.drive(t , c.io.enq, c.clock)
      }
      c.clock.step(5)
      val recvTxns = mutable.ListBuffer[MonTx[UInt]]()
      for (i <- 0 until 4) {
        recvTxns.addOne(QueueReciever.receive(c.io.deq, c.clock, gen)) 
      }
      /*
      val recvTxns = (0 until 4).foldLeft(Seq[Transaction[UInt]]()){ (seq, i) =>
        seq :+ QueueReciever.receive(c.io.deq, c.clock, gen)  // repeat 4 times with (0 until 4) and what is "i?
      }
      */
      c.clock.step(1)
      println(recvTxns)
      c.clock.step(5)
    }
  }

  "run a parallel test" in {
    val gen = UInt(32.W)
    test(new Queue(gen, 8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val txns = (0 until 10).map{ i =>
        new DrvTx(gen).Lit(_.bits -> i.U, _.preDelay -> 0.U, _.postDelay -> 0.U)
      }
      val recvTxns = mutable.ListBuffer[MonTx[UInt]]()
      fork {
        txns.foreach { t =>
          QueueDriver.drive(t, c.io.enq, c.clock)
          c.clock.step(1)
        }
      }.fork{
        c.clock.step(200)
        for (i <- (0 until 10)) {
          recvTxns.addOne(QueueReciever.receive(c.io.deq, c.clock, gen))
        }
      }.join()
      println(recvTxns)
    }
  }

    "run a monitor test" in {
    val gen = UInt(32.W)
    test(new Queue(gen, 8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val txns = (0 until 10).map{ i =>
        new DrvTx(gen).Lit(_.bits -> i.U, _.preDelay -> 1.U, _.postDelay -> 2.U)
      }
      val recvTxns = mutable.ListBuffer[MonTx[UInt]]()
      val mDrvThread = fork {
        txns.foreach { t =>
          QueueDriver.drive(t, c.io.enq, c.clock)
          c.clock.step(1)
        }
      }
      val sDrvThread = fork {
        c.clock.step(20)
        for (i <- (0 until 10)) {
          recvTxns.addOne(QueueReciever.receive(c.io.deq, c.clock, gen))
        }
      }

      fork.withRegion(Monitor){
        println("IN thE MONITOR")
        val monitor = new QueueMonitor()
        while (true) {
          val p = monitor.receiveOne(c.io.enq, c.clock, gen)
          println(p)
        }
      }

      mDrvThread.join()
      sDrvThread.join()
      //println(recvTxns)
    }
  }

}

