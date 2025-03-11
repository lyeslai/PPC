package upmc.akka.leader

import akka.actor.{Actor, Props}


// New message type
case class StartPlayerAtTerminal(terminal: Terminal)
case class StartOnlyLive(aliveList: List[Terminal])

class ChefOrchestre(val id: Int, val terminaux: List[Terminal]) extends Actor {
  val providerActor = context.actorOf(Props[ProviderActor], "provider")
  var started: Boolean = false

  override def receive: Receive = {
    case StartOnlyLive(aliveList) =>
      if (!started) {
        started = true
        println(s"ChefOrchestre $id reçu aliveList: " + aliveList.mkString(", "))
        // Only consider alive musicians that are not the chef himself:
        val filtered = aliveList.filter(_.id != id)
        if (filtered.nonEmpty) {
          val random = new scala.util.Random
          val terminal = filtered(random.nextInt(filtered.length))
          providerActor ! StartPlayerAtTerminal(terminal)
        } else {
          println("Pas assez de musiciens vivants, arrêt du spectacle.")
          context.system.terminate()
        }
      } else {
        println(s"ChefOrchestre $id a déjà démarré, ignorer StartOnlyLive.")
      }
  }
}
