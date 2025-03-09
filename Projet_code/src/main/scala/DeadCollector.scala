package upmc.akka.leader

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class DeadCollector(val terminaux: List[Terminal]) extends Actor {


  var aliveMusicians: List[Terminal] = terminaux

  context.system.scheduler.schedule(0.seconds, 30.seconds, self, CheckAlive)

  override def receive: Receive = {
    case CheckAlive =>
      aliveMusicians.foreach { terminal =>
        val musician = context.actorSelection(s"akka.tcp://MozartSystem${terminal.id}@${terminal.ip}:${terminal.port}/user/Musicien${terminal.id}")
        musician ! Ping
      }

    case Pong(terminal) =>
      if (!aliveMusicians.contains(terminal)) {
        aliveMusicians = terminal :: aliveMusicians
      }

    case Terminated(musician) =>
      aliveMusicians = aliveMusicians.filter(_.id != musician.path.name.toInt)
      println(s"Musicien ${musician.path.name} est mort.")

      if (musician.path.name == "ChefOrchestre") {
        context.system.actorSelection("/user/Musicien*") ! ElectNewConductor
      }
  }
}

case object CheckAlive
case object Ping
case class Pong(terminal: Terminal)
case object ElectNewConductor