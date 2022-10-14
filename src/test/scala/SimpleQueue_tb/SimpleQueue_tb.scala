package SimpleQueue_tb

import SimpleQueue
import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chiseltest._
import chisel3.util._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.{TreadleBackendAnnotation, WriteVcdAnnotation}

class QueueTest extends AnyFlatSpec with ChiselScalatestTester {
  def test(new Queue(8, UInt(32.W))){ c => // call drive with some Transactions }
    val io = IO(new Bundle {
      val producer = Decoupled(UInt(32.W))
      val consumer = Flipped(Decoupled(UInt(32.W)))  // how to deal with a sequence of data in transaction?
    })

    //one data transaction
    // Producer := (new Decoupled).Lit(_.valid -> true.B, _.ready -> true.B, _.data->8.U ) // can I write f.U here?
    consumer := (new Decoupled).Lit(_.bits -> 0.U, _.ready -> true.B, _.valid -> false.B) 
    val tx = Transaction(32).Lit(_.bits -> 100.U)
    val car = new Queue(tx, consumer) //difference between decoupledIO and decoupled.
    car.drive(tx, consumer)



}
