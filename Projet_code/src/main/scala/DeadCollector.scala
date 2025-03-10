package upmc.akka.leader

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class DeadCollector(val terminaux: List[Terminal]) extends Actor {

  var aliveMusicians: List[Terminal] = terminaux

  override def preStart(): Unit = {
    terminaux.foreach { terminal =>
      context.actorSelection(
        s"akka.tcp://MozartSystem${terminal.id}@${terminal.ip}:${terminal.port}/user/Musicien${terminal.id}"
      ).resolveOne(5.seconds).foreach(context.watch)
    }
    context.system.scheduler.schedule(0.seconds, 5.seconds, self, CheckAlive)
  }

  def receive: Receive = {
    case CheckAlive =>
      terminaux.foreach { terminal =>
        context.actorSelection(
          s"akka.tcp://MozartSystem${terminal.id}@${terminal.ip}:${terminal.port}/user/Musicien${terminal.id}"
        ) ! Ping
      }
      if (aliveMusicians.size == 1) {
        println("⏳ Arrêt prévu dans 30 secondes.")
        context.system.scheduler.scheduleOnce(30.seconds)(context.system.terminate())
      }

    case Pong(terminal) =>
      if (!aliveMusicians.contains(terminal))
        aliveMusicians = terminal :: aliveMusicians

    case Terminated(actorRef) =>
      val deadId = actorRef.path.name.stripPrefix("Musicien").toInt
      aliveMusicians = aliveMusicians.filterNot(_.id == deadId)
      context.actorSelection("/user/Musicien*") ! MusicianFailed(deadId)
      context.actorSelection("/user/Musicien*") ! ElectNewConductor
  }
}
