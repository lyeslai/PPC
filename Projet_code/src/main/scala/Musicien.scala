package upmc.akka.leader

import akka.actor._
import scala.concurrent.ExecutionContext.Implicits.global

import javax.sound.midi._
import javax.sound.midi.ShortMessage._
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global


class Musicien(val id: Int, val terminaux: List[Terminal]) extends Actor {

  val displayActor = context.actorOf(Props[DisplayActor], "displayActor")
  val playerActor = context.actorOf(Props[PlayerActor], "playerActor")
  val chefOrchestre = context.actorOf(Props(new ChefOrchestre(id, terminaux)), "chefOrchestre")

  var aliveMusicians = terminaux

  override def preStart(): Unit = {
    terminaux.filterNot(_.id == id).foreach { terminal =>
      context.actorSelection(
        s"akka.tcp://MozartSystem${terminal.id}@${terminal.ip}:${terminal.port}/user/Musicien${terminal.id}"
      ).resolveOne(5.seconds).onComplete {
        case scala.util.Success(actorRef) => context.watch(actorRef)
        case scala.util.Failure(_) => // Ignore les erreurs
      }
    }
  }

 def receive = {
  case Start =>
    displayActor ! Message(s"Musicien $id is created")
    if (id == terminaux.map(_.id).min) {
      chefOrchestre ! Start
      displayActor ! Message(s"Musicien $id est le chef d'orchestre initial.")
    } else {
      displayActor ! Message(s"Musicien $id attend le chef d'orchestre.")
    }

  case Play_Measure(measure) =>
    // Seul le chef d'orchestre envoie des mesures
    if (id == aliveMusicians.map(_.id).min) {
      playerActor ! measure
    }

  case Terminated(actorRef) =>
    val deadId = actorRef.path.name.stripPrefix("Musicien").toInt
    aliveMusicians = aliveMusicians.filterNot(_.id == deadId)
    displayActor ! Message(s"Le musicien $deadId est mort.")
    self ! ElectNewConductor

  case ElectNewConductor =>
    val newLeaderId = aliveMusicians.map(_.id).min
    if (id == newLeaderId) {
      chefOrchestre ! Start
      displayActor ! Message(s"🎉 Musicien $id est élu nouveau chef.")
    }

  case MusicianFailed(failedId) =>
    aliveMusicians = aliveMusicians.filterNot(_.id == failedId)
    displayActor ! Message(s"Le musicien $failedId est mort.")
}
}


object PlayerActor {
  case class MidiNote(pitch: Int, vel: Int, dur: Int, at: Int)
  val info = MidiSystem.getMidiDeviceInfo().filter(_.getName == "Gervill").headOption
  // or "SimpleSynth virtual input" or "Gervill"
  val device = info.map(MidiSystem.getMidiDevice).getOrElse {
    println("[ERROR] Could not find Gervill synthesizer.")
    sys.exit(1)
  }

  val rcvr = device.getReceiver()

  def note_on(pitch: Int, vel: Int, chan: Int): Unit = {
    val msg = new ShortMessage
    msg.setMessage(NOTE_ON, chan, pitch, vel)
    rcvr.send(msg, -1)
  }

  def note_off(pitch: Int, chan: Int): Unit = {
    val msg = new ShortMessage
    msg.setMessage(NOTE_ON, chan, pitch, 0)
    rcvr.send(msg, -1)
  }
}

class PlayerActor() extends Actor {

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

    case MidiNote(p, v, d, at) =>
      context.system.scheduler.scheduleOnce((at) milliseconds)(note_on(p, v, 10))
      context.system.scheduler.scheduleOnce((at + d) milliseconds)(note_off(p, 10))
  }
}