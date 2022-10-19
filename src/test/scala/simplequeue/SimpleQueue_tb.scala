package simplequeue

import chisel3._
import chiseltest._
import chisel3.util._
import org.scalatest.freespec.AnyFreeSpec
//import chiseltest.experimental.TestOptionBuilder._
import chiseltest.{TreadleBackendAnnotation, WriteVcdAnnotation}
import chisel3.experimental.BundleLiterals._

class QueueTest extends AnyFreeSpec with ChiselScalatestTester {
  "run a test" in {
    test(new Queue(UInt(32.W), 8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.clock.step(10)
      QueueDriver.drive(new Transaction(32).Lit(b => b.bits -> 100.U), c.io.enq, c.clock)
      c.clock.step(5)
      //val peeked = c.io.deq.bits.peek()
      //import peeked.implicits._
      //printf(s"peeked (s) = %d\n", peeked)
      //printf(cf"peeked (cf) = $peeked")
      val t = QueueReciever.receive(c.io.deq, c.clock)
      c.clock.step(1)
      //printf("t.bits.litValue = %d/n", t.bits.litValue)
      printf(p" t.bits.litValue = $t.bits.litValue")
      assert(t.bits.litValue == 100, "the value was not 100")
      c.clock.step(5)

      // TODO: use chiseltest fork/join to drive and receive in parallel
      // TODO: define a software model of queue
    }
  }
    /*
    test(new Queue(8, UInt(32.W))){ c => // call drive with some Transactions }
      val io = IO(new Bundle {
        val producer = Decoupled(UInt(32.W))
        val consumer = Flipped(Decoupled(UInt(32.W)))  // how to deal with a sequence of data in transaction?
      })
  */


  "run a parallel test" in {
    test(new Queue(UInt(32.W), 8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
    val testVector = Seq.tabulate(10){ i => i.U }
    
      fork{
        for (a <- testVector) {
        QueueDriver.drive(new Transaction(32).Lit(_ -> a), c.io.enq, c.clock)
        }
      }.fork{
        for (b <- testVector) {
        val t = QueueReciever.receive(c.io.deq, c.clock)
        assert(t.bits.litValue == b, "the value was not 100")
        c.clock.step(2)
        }
      }.join()
    }
  }
}
/*
    //one data transaction
    // Producer := (new Decoupled).Lit(_.valid -> true.B, _.ready -> true.B, _.data->8.U ) // can I write f.U here?
    consumer := (new Decoupled).Lit(_.bits -> 0.U, _.ready -> true.B, _.valid -> false.B) 
    val tx = Transaction(32).Lit(_.bits -> 100.U)
    val car = new Queue(tx, consumer) //difference between decoupledIO and decoupled.
    car.drive(tx, consumer)



}
*/