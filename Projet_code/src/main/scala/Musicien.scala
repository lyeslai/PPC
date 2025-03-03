package upmc.akka.leader

import akka.actor.TypedActor.dispatcher
import akka.actor._
import upmc.akka.leader.DataBaseActor.{Elected_Leader, Play_Measure}

import javax.sound.midi._
import javax.sound.midi.ShortMessage._
import scala.concurrent.duration.DurationInt


case class Start ()

class Musicien (val id:Int, val terminaux:List[Terminal]) extends Actor {

     // Les differents acteurs du systeme
     val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor")
     val playerActor = context.actorOf(Props[PlayerActor], name = "playerActor")
     val chefOrchestre = context.actorOf(Props(new ChefOrchestre(id, terminaux)), name = "chefOrchestre")

     // TODO : envoie message à actor MORT ( pour collecter si il est mort ou pas)


     def receive = {

          // Initialisation
          case Start => {
               displayActor ! Message ("Musicien " + this.id + " is created")
          }

          case Play_Measure(measure) => {
               playerActor ! measure
          }

          //case Mort_Collector => {
               // TODO
          //}

         // elected leader
          case Elected_Leader() => {
               // act as choreographer
               chefOrchestre ! Start
          }

          //case new_Musician(terminal: Terminal) => {
                // TODO
          //}

     }
}
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

//////////////////////////////////////////////////

class PlayerActor () extends Actor {

     import DataBaseActor._
     import PlayerActor._

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


