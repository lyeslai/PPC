package actors

import akka.actor._

case object PulseCheck

case class PulseResponse(id: Int, isConductor: Boolean)

case object SetAsConductor

class PulseActor(val musicianId: Int) extends Actor {
  private var isConductor: Boolean = false
  
  def receive: Receive = {
    case PulseCheck =>
      sender() ! PulseResponse(musicianId, isConductor)
      
    case SetAsConductor =>
      isConductor = true
  }
}
