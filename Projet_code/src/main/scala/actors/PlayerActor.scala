package actors

import math._
import javax.sound.midi._
import javax.sound.midi.ShortMessage._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import mozart.models.Measure


object PlayerActor {
  case class MidiNote (pitch:Int, vel:Int, dur:Int, at:Int)
  val info = MidiSystem.getMidiDeviceInfo().filter(_.getName == "Gervill").headOption
  // or "SimpleSynth virtual input" or "Gervill"
  val device = info.map(MidiSystem.getMidiDevice).getOrElse {
    println("[ERROR] Could not find Gervill synthesizer.")
    sys.exit(1)
  }

  val rcvr = device.getReceiver

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

//////////////////////////////////////////////////

class PlayerActor () extends Actor{
  import DataBaseActor._
  import PlayerActor._
  device.open()

  def receive: Receive = {
    case Measure (l) => {
      println("received measure")
      for (i <- l.indices){
        val cursorChord = l(i)
        val at = cursorChord.time

        for (j <- cursorChord.notes.indices){
          val cursorNote = cursorChord.notes(j)
          context.self ! MidiNote(cursorNote.pitch, cursorNote.volume, cursorNote.duration, at)
        }
      }
    }
    case MidiNote(p,v, d, at) => {
      context.system.scheduler.scheduleOnce ((at).milliseconds) (note_on (p,v,10))
      context.system.scheduler.scheduleOnce ((at+d).milliseconds) (note_off (p,10))
    }
  }
}