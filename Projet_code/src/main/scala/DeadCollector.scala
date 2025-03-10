package upmc.akka.leader

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class DeadCollector(val terminaux: List[Terminal]) extends Actor {
  var aliveMusicians: List[Terminal] = terminaux

  override def preStart(): Unit = {
    context.system.scheduler.schedule(0.seconds, 5.seconds, self, Ping)
  }

  def receive: Receive = {
    case Ping =>
      terminaux.foreach { terminal =>
        context.actorSelection(
          s"akka.tcp://MozartSystem${terminal.id}@${terminal.ip}:${terminal.port}/user/Musicien${terminal.id}"
        ) ! Ping
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