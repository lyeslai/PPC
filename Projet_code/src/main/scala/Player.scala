package upmc.akka.leader

import akka.actor.Actor
import upmc.akka.leader.DataBaseActor.Measure
import akka.actor.TypedActor.dispatcher
import akka.actor._
import upmc.akka.leader.PlayerActor.{MidiNote, device, note_off, note_on}

import javax.sound.midi._
import javax.sound.midi.ShortMessage._
import scala.concurrent.duration.DurationInt
import javax.sound.midi.ShortMessage.NOTE_ON
import javax.sound.midi.{MidiSystem, ShortMessage}

object PlayerActor {
  case class MidiNote (pitch:Int, vel:Int, dur:Int, at:Int)
  val info = MidiSystem.getMidiDeviceInfo().filter(_.getName == "Gervill").headOption
  // or "SimpleSynth virtual input" or "Gervill"
  val device = info.map(MidiSystem.getMidiDevice).getOrElse {
    println("[ERROR] Could not find Gervill synthesizer.")
    sys.exit(1)
  }

  val rcvr = device.getReceiver()

  /////////////////////////////////////////////////
  def note_on (pitch:Int, vel:Int, chan:Int): Unit = {
    val msg = new ShortMessage
    msg.setMessage(NOTE_ON, chan, pitch, vel)
    rcvr.send(msg, -1)
  }

  def note_off (pitch:Int, chan:Int): Unit = {
    val msg = new ShortMessage
    msg.setMessage(NOTE_ON, chan, pitch, 0)
    rcvr.send(msg, -1)
  }

}

class PlayerActor () extends Actor {

  device.open()

  def receive = {
    case Measure(chords) =>
      println("Joue une mesure...")
      chords.foreach { chord =>
        chord.notes.foreach { note =>
          self ! MidiNote(note.pitch, note.vol, note.dur, chord.date)
        }
      }

    case MidiNote(p, v, d, at) => {
      context.system.scheduler.scheduleOnce((at) milliseconds)(note_on(p, v, 10))
      context.system.scheduler.scheduleOnce((at + d) milliseconds)(note_off(p, 10))
    }
  }
}


