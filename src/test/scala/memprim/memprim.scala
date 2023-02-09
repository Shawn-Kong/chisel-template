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

case class Channel(channels: Map[String, Seq[Byte]]) extends collection.mutable.Map
// map("A") = Seq[Byte]
// map("B") = Seq[Byte]
// map("C") = Seq[Byte]
// map("D") = Seq[Byte]
// map("E") = Seq[Byte]
case class Read(addr: BigInt) extends MemPrim
case class ReadResponse(data: Seq[Byte]) extends MemPrim
case class Write(addr: BigInt, data: Seq[Byte]) extends MemPrim

sealed abstract class TLPrim(dataWidth: Int)
case class Get(addr: BigInt, corrupt) extends TLPrim
case class Put(data: BigInt, addr: BitInt, corrupt) extends TLPrim