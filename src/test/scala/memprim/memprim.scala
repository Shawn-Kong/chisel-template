import chisel3._

sealed trait MemPrim {
    def memReadtoTLGet[T <: Data](readCommand: Read, channel: Channel): TLPrim = {
        val addr: Seq[Byte] = readCommand.addr
        channel("A") = ("Get", addr) // put request on channel
        while (true){
            if (channel.get("D") == "GetAccessAck"){
                val data = channel("D")    // wait until there are returned data
                data
            }
        }

    }

    def memWritetoTLPut[T <: Data](writeCommand: Write, channel: Channel): TLPrim = {
        val corrupt: Boolean = false
        val addr: BitInt = writeCommand.addr
        val data: Seq[Byte] = writeCommand.data
        channel("A") = ("Put", addr, data)
        
    }
}

case class TLBundleParameters(
  addressBits: Int,
  dataBits:    Int,
  sourceBits:  Int,
  sinkBits:    Int,
  sizeBits:    Int)

object MemPrim {
    def translateToTL(mp: MemPrim, tlBundleParams: TLBundleParameters): Seq[TLPrim] = {
        mp match {
            case Read(addr, nBytes) =>
                if (nBytes == tlBundleParams.dataBits*8) {
                    Get(addr, tlBundleParams.dataBits)
                } else {
                    // This is a Get + another (or more) Get
                    ???
                }
            case Write(addr, data: Seq[Byte]) =>
                // TODO: this is wrong - the types don't match up
                if (data.length*8 == dataBits) {
                    PutFullData(data.toByteArray(), addr, tlBundleParams.dataBits)
                } else if (data.length*8 < dataBits) {
                    ??? // this must be a PutPartial
                } else { // we have more data than can fit on one bus beat
                    ??? // this must be a PutFullData + Seq[PutFullData] + PutPartialData
                }
        }
    }
}

case class Channel(channels: Map[String, Seq[Byte]]) extends collection.mutable.Map
// map("A") = Seq[Byte]
// map("B") = Seq[Byte]
// map("C") = Seq[Byte]
// map("D") = Seq[Byte]
// map("E") = Seq[Byte]
case class Read(addr: BigInt, nBytes: Int) extends MemPrim
//case class ReadResponse(data: Seq[Byte]) extends MemPrim
case class Write(addr: BigInt, data: Seq[Byte]) extends MemPrim

sealed abstract class TLPrim(dataWidth: Int)
case class Get(addr: BigInt, dataWidth: Int) extends TLPrim(dataWidth)
case class PutFullData(data: BigInt, addr: BitInt, dataWidth: Int) extends TLPrim(dataWidth)
case class PutPartialData(data: BigInt, addr: BitInt, dataWidth: Int) extends TLPrim(dataWidth)
