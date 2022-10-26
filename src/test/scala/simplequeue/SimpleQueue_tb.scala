package simplequeue

import chisel3._
import chiseltest._
import chisel3.util._
import org.scalatest.freespec.AnyFreeSpec
//import chiseltest.experimental.TestOptionBuilder._
import chiseltest.{TreadleBackendAnnotation, WriteVcdAnnotation}
import chisel3.experimental.BundleLiterals._

import scala.collection.mutable

class QueueTest extends AnyFreeSpec with ChiselScalatestTester {
  "run a test" in {
    val gen = UInt(32.W)
    test(new Queue(gen, 8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.clock.step(10)
      val txns = (0 until 4).map{ i =>
        new Transaction(gen).Lit(b => b.bits -> (100+i).U)
      }
      txns.foreach { t=>
        QueueDriver.drive(t , c.io.enq, c.clock)
      }
      c.clock.step(5)
      val recvTxns = mutable.ListBuffer[Transaction[UInt]]()
      for (i <- 0 until 4) {
        recvTxns.addOne(QueueReciever.receive(c.io.deq, c.clock, gen))  // repeat 4 times with (0 until 4) and what is "i?
      }
      /*
      val recvTxns = (0 until 4).foldLeft(Seq[Transaction[UInt]]()){ (seq, i) =>
        seq :+ QueueReciever.receive(c.io.deq, c.clock, gen)  // repeat 4 times with (0 until 4) and what is "i?
      }
      */
      c.clock.step(1)
      println(recvTxns)
      c.clock.step(5)
      // TODO: use chiseltest fork/join to drive and receive in parallel
      // TODO: define a software model of queue
    }
  }

  "run a parallel test" in {
    val gen = UInt(32.W)
    test(new Queue(gen, 8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val txns = (0 until 10).map{ i =>
        new Transaction(gen).Lit(_.bits -> (i).U)
      }
      val recvTxns = mutable.ListBuffer[Transaction[UInt]]()
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
}

  /*"run a parallel test" in {
    test(new Queue(UInt(32.W), 8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
    val testVector = Seq.tabulate(7){ i => i.U }
    // println(testVector)
    
      fork{
        for (a <- testVector) {
        QueueDriver.drive(new Transaction(32).Lit(_ -> a), c.io.enq, c.clock)
        }
        c.clock.step(1)
      }.fork{
        c.clock.step(2)
        val t = QueueReciever.receive(c.io.deq, c.clock)
        println(t)
        }.join()
    }
  }
}*/
/*
    //one data transaction
    // Producer := (new Decoupled).Lit(_.valid -> true.B, _.ready -> true.B, _.data->8.U ) // can I write f.U here?
    consumer := (new Decoupled).Lit(_.bits -> 0.U, _.ready -> true.B, _.valid -> false.B) 
    val tx = Transaction(32).Lit(_.bits -> 100.U)
    val car = new Queue(tx, consumer) //difference between decoupledIO and decoupled.
    car.drive(tx, consumer)



}
*/
