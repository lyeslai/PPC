package actors

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

case object CheckAllMusicians

case object DisplayStatus

/** Internal message to maintain musician status */
case class MusicianStatusUpdate(id: Int, status: Int)

/**
 * Actor responsible for monitoring the status of all musicians
 * Regularly checks if musicians are alive and updates the parent actor
 */
class MonitorActor(val musicianId: Int, val pulseActor: ActorRef) extends Actor {
  private val console = context.actorOf(Props[DisplayActor], "console")
  private val checkInterval = 900.milliseconds
  private val statusInterval = 1000.milliseconds
  private val maxMissedChecks = 5
  
  // Status: -1 = offline, 0 = online, 1 = conductor
  private var musicianStatus = Array.fill[Int](4)(-1)
  private var missedChecksCount = new Array[Int](4)
  
  // Initialize this musician as online
  musicianStatus(musicianId) = 0
  
  // Get references to all other musicians' pulse actors
  private val musicianPulseActors = Array.tabulate[ActorSelection](4) { i =>
    context.actorSelection(s"akka.tcp://MozartSystem$i@127.0.0.1:600$i/user/musician$i/pulse")
  }
  
  override def preStart(): Unit = {
    // Schedule regular status checks
    context.system.scheduler.schedule(0.seconds, checkInterval, self, CheckAllMusicians)
    context.system.scheduler.schedule(0.seconds, statusInterval, self, DisplayStatus)
  }
  
  def receive: Receive = {
    case DisplayStatus =>
      console ! Message(s"Musician status: [${musicianStatus.mkString(",")}]")
      
    case CheckAllMusicians =>
      (0 until 4).foreach { i =>
        if (i != musicianId) {
          // Check other musicians
          musicianPulseActors(i) ! PulseCheck
          missedChecksCount(i) += 1
          
          // If maximum missed checks reached, mark as offline
          if (missedChecksCount(i) == maxMissedChecks) {
            musicianStatus(i) = -1
            context.parent ! MusicianStatusUpdate(i, -1)
            missedChecksCount(i) = 0
          }
        } else {
          // Check self
          pulseActor ! PulseCheck
        }
      }
      
    case PulseResponse(id, isConductor) =>
      missedChecksCount(id) = 0
      val newStatus = if (isConductor) 1 else 0
      musicianStatus(id) = newStatus
      context.parent ! MusicianStatusUpdate(id, newStatus)
  }
}
