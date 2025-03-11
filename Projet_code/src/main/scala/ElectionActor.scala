package upmc.akka.leader

import akka.actor._

case class Election(list: List[Int])

class ElectionActor(val terminaux : List[Terminal]) extends  Actor with Broadcast {


  // In ElectionActor.scala - Make node 0 the leader
  override def receive: Receive = {
    case Start =>
      println("Starting election system")

    case Election(allTerminaux) => {

      if (allTerminaux.size == 1) {
        println("Only one node in the system, it's the leader")
        broadcastLeader(allTerminaux.head)
      } else {
        println("Election started with nodes: " + allTerminaux.mkString(", "))
        // Otherwise use random election
        val random = new scala.util.Random
        var rand = random.nextInt(allTerminaux.length)
        val leader = allTerminaux( rand )
        println("Leader is " + leader)
        broadcastLeader(leader)
      }

    }
  }
}

