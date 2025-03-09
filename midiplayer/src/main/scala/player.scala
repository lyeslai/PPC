import javax.sound.midi._
import scala.collection.JavaConversions._

object MidiReceiver extends Receiver {
  // Get the default synthesizer
  val synth: Synthesizer = MidiSystem.getSynthesizer
  synth.open()
  val synthReceiver: Receiver = synth.getReceiver

  // Called when a MIDI message is received
  override def send(message: MidiMessage, timeStamp: Long): Unit = {
    println(s"Received MIDI message: ${message.getMessage.mkString(" ")} at $timeStamp")
    synthReceiver.send(message, timeStamp) // Forward the message to the synthesizer
  }

  override def close(): Unit = {
    synthReceiver.close()
    synth.close()
  }

  def main(args: Array[String]): Unit = {
    val devices = MidiSystem.getMidiDeviceInfo
    println("Available MIDI Devices:")
    devices.zipWithIndex.foreach { case (info, index) =>
      println(s"[$index] ${info.getName} - ${info.getDescription}")
    }

    print("Select a MIDI device index to open: ")
    val selectedIndex = scala.io.StdIn.readInt()

    val device: MidiDevice = MidiSystem.getMidiDevice(devices(selectedIndex))
    device.open()

    val transmitters = device.getTransmitters()
    if (transmitters.isEmpty) {
      device.getTransmitter.setReceiver(this)
    } else {
      transmitters.foreach (x => x.setReceiver(this))
    }

    println(s"Listening for MIDI messages on ${devices(selectedIndex).getName}... Press Enter to exit.")
    scala.io.StdIn.readLine()

    device.close()
    close()
  }
}