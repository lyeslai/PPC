package upmc.akka.leader

import akka.actor.{Actor, Props}

import scala.util.Random

// New message type
case class StartPlayerAtTerminal(terminal: Terminal)

class ChefOrchestre (val id:Int, val terminaux:List[Terminal]) extends Actor {
  val providerActor = context.actorOf(Props[ProviderActor], "provider")

  override def receive: Receive = {
    case Start => {
      println("ChefOrchestre " + this.id + " is created")
      // select a random terminal to start the music
      val random = new Random()
      val terminal = terminaux(random.nextInt(terminaux.length))

      // Send the terminal info directly - don't try to resolve the actor
      providerActor ! StartPlayerAtTerminal(terminal)
    }
  }
}