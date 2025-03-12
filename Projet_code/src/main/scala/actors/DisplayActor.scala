package actors

import akka.actor._

case class Message (content:String)

class DisplayActor extends Actor {

  def receive: Receive = {

    case Message (content) => {
      println(content)
    }

  }
}