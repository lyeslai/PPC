package upmc.akka.leader


import akka.actor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
case object Presence_report


class ReporterActor() extends Actor {
  val beatInterval: FiniteDuration = 3.second
  val father = context.parent

  def receive: Receive = {
    case Start => schedulePulse()

    case Presence_report =>
      father ! Presence_report
      schedulePulse()
  }


  private def schedulePulse(): Unit = {
    context.system.scheduler.scheduleOnce(beatInterval, self, Presence_report)
  }
}