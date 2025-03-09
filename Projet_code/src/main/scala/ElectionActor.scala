package upmc.akka.leader

import akka.actor._

case class Election(list: List[Int])

class ElectionActor(val terminaux : List[Terminal]) extends  Actor with Broadcast {


  // In ElectionActor.scala - Make node 0 the leader
  override def receive: Receive = {
    case Start =>
      println("Starting election system")

    case Election(allTerminaux) => {
      println("Election started with nodes: " + allTerminaux.mkString(", "))
      // If there is only one node, it is the leader
      if (allTerminaux.length == 1) {
        println("Only one node, it is the leader")
        broadcastLeader(allTerminaux.head)
      } else {
        // Otherwise use random election
        val random = new scala.util.Random
        val leader = allTerminaux(random.nextInt(allTerminaux.length))
        println("Leader is " + leader)
        broadcastLeader(leader)
      }
    }
  }
}

