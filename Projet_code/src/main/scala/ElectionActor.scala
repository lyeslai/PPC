package upmc.akka.leader

import akka.actor._

case class Election(list: List[Int])

class ElectionActor(val terminaux : List[Terminal]) extends  Actor with Broadcast {


  override def receive: Receive = {
    case Start =>
      println("Starting election system")

    case Election(allTerminaux) =>
      if (allTerminaux.isEmpty) {
        println("Election: No candidates available. Skipping election.")
      } else if (allTerminaux.size == 1) {
        println("Only one node in the system, it's the leader")
        broadcastLeader(allTerminaux.head)
      } else {
        println("Election started with nodes: " + allTerminaux.mkString(", "))
        // Use a deterministic method (for example, choose the smallest id)
        val leader = allTerminaux.min
        println("Leader is " + leader)
        broadcastLeader(leader)
      }

  }


}

