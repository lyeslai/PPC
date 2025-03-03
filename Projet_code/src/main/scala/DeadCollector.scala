package upmc.akka.leader

import akka.actor._
import upmc.akka.leader.Terminal

class DeadCollector (val terminaux:List[Terminal]) extends Actor {

  override def receive: Receive = ???
}
