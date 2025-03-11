package upmc.akka.leader

import akka.actor.TypedActor.dispatcher
import akka.actor.{Actor, Props}
import upmc.akka.leader.DataBaseActor.Measure

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.control.Breaks.break


// New message type
case class StartPlayerAtTerminal(terminal: Terminal)
case class LeaderFound(id: Int)

class ChefOrchestre(val id: Int, val terminaux: List[Terminal]) extends Actor {
  val providerActor = context.actorOf(Props[ProviderActor], "provider")
  var seconds_elapsed = 0
  var isLeader = false
  var musiciansAlive: List[Int] = List()
  var currentMusicianPlaying: Int = -1
  var hasLaunched: Boolean = false


  override def receive: Receive = {

    case LeaderFound(id) =>
      println(s"ChefOrchestre $id reçu LeaderFound")
      if (id == this.id) {
        println("ChefOrchestre est le leader.")
        this.isLeader = true
      }

//      reportCurrentAlive
    case reportCurrentAlive(aliveList) =>
      println("ChefOrchestre reçu reportCurrentAlive")
      this.musiciansAlive = aliveList
      println("Musiciens vivants : " + this.musiciansAlive.mkString(", "))

    case Musician_dead(id) =>
      println(s"ChefOrchestre reçu Musician_dead($id)")
      this.musiciansAlive = this.musiciansAlive.filter(_ != id)
      if (currentMusicianPlaying == id) {
        println(s"Musicien $id était en train de jouer, arrêt du spectacle.")
        // launch check musicians
        self ! CheckMusicians
        providerActor ! StopP
      }


//    case CheckMusicians =>
//      println("Vérification des musiciens...")
//      this.seconds_elapsed += 5
//      if(musiciansAlive.size > 1) {
//        var filtered = musiciansAlive.filter( _ != id ).min
//        currentMusicianPlaying = filtered
//        providerActor ! StartPlayerAtTerminal( terminaux(filtered) )
//        this.seconds_elapsed = 0
//      } else if (this.seconds_elapsed < 30) {
//        println("Aucun musicien disponible, réessai dans 5s.")
//        context.system.scheduler.scheduleOnce(5.seconds, self, CheckMusicians)(context.dispatcher)
//      } else {
//        println("Aucun musicien disponible, arrêt du spectacle.")
//        context.system.terminate()
//      }

    case EndMeasure =>
      println("ChefOrchestre reçu EndMeasure")
      hasLaunched = false

    case CheckMusicians =>
      if (!hasLaunched) {
        println("Vérification des musiciens...")
        this.seconds_elapsed += 5
        if (musiciansAlive.size > 1) {
          // Choose the smallest id among the others
          val filtered = musiciansAlive.filter(_ != id)
          if (filtered.nonEmpty) {
            val nextMusicianId = filtered.min
            currentMusicianPlaying = nextMusicianId
            println(s"Starting music with musician $nextMusicianId")
            providerActor ! StartPlayerAtTerminal(terminaux(nextMusicianId))
            hasLaunched = true  // lock until measure finishes
            this.seconds_elapsed = 0
          }
        } else if (this.seconds_elapsed < 30) {
          println("Aucun musicien disponible, réessai dans 5s.")
          context.system.scheduler.scheduleOnce(5.seconds, self, CheckMusicians)(context.dispatcher)
        } else {
          println("Aucun musicien disponible, arrêt du spectacle.")
          context.system.terminate()
        }
      } else {
        println("ChefOrchestre déjà lancé, ignorer CheckMusicians.")
      }




      }



}


