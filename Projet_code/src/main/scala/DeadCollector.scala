package upmc.akka.leader

import akka.actor._

import java.util.Date
import scala.concurrent.duration._

abstract class CheckerMessage
case object CheckerTick extends CheckerMessage

class DeadCollector (val terminaux:List[Terminal], electionActor: ActorRef) extends Actor with Broadcast {
  val checkInterval: FiniteDuration = 2500.milliseconds // interval for checking
  val deathThreshold: Int = 3 // number of checks before a node is considered failed

  var isThereLeader: Boolean = false
  var musiciansAlive: Map[Int, Date] = Map() // stores nodes and their last active time
  var leader: Int = -1 // ID of the current leader
  // chaque 5 secondes, le musicien vérifie s'il est seul
  var aloneCheckingInterval: Int = 5
  var aloneCheckingCounter: Int = 6


  override def receive: Receive = {
    case Start => {
      context.system.scheduler.scheduleOnce(checkInterval, self, CheckerTick)(context.dispatcher)
    }

    case Check_leader => {
      if (leader != -1) {
        val terminalsIds = terminaux.filter(t => musiciansAlive.contains(t.id)).map(_.id)
        electionActor ! Election(terminalsIds)
      }else{
        println("Leader is already there")
        sender() ! Leader_found(leader)
      }
    }

    case Report_presence(id, isLeader) => {
      if (leader == -1) {
        // if there is no leader, start an election with all nodes alive
        electionActor ! Election(terminaux.filter(t => musiciansAlive.contains(t.id)).map(_.id))
      }

      if (isLeader) {
        isThereLeader = true
        leader = id
      }

      // Only update if this is new information or refreshed timestamp
      val lastSeen = musiciansAlive.get(id)
      val now = new Date()

      if (!musiciansAlive.contains(id)) { // New musician
        println(s"New musician detected: $id")
        sendNewMusician(id)
      }

      // Update last seen time
      musiciansAlive += (id -> now)

      // Only broadcast if this is new information or significant time has passed
      if (lastSeen.isEmpty || now.getTime - lastSeen.get.getTime > checkInterval.toMillis) {
        // Broadcast to other nodes but prevent re-broadcasting
        broadcastPresence(id, isLeader)
      }
    }

    case Check_end => {
      if (aloneCheckingCounter == 0) {
        // if the node is alone, it's elected as leader
        electionActor ! Election(List(terminaux.head.id))
        }

      if (musiciansAlive.size >1) {
        aloneCheckingCounter = 6
      } else {
        aloneCheckingCounter -= 1
        context.system.scheduler.scheduleOnce(aloneCheckingInterval.seconds, self, Check_end)(context.dispatcher)
        }
      }

    case CheckerTick => updateMusiciansAlive()

  }

  private def updateMusiciansAlive(): Unit = {
    val now = new Date
    musiciansAlive.foreach { case (musicianId, lastAlive) =>
      // if a node has not reported activity within the allowed time, consider it is left
      if (now.getTime - lastAlive.getTime > deathThreshold * checkInterval.toMillis) {
        musiciansAlive -= musicianId
        if (musicianId == leader) {
          println("Leader non réactif, déclenchement d'une nouvelle élection")
          electionActor ! Election(musiciansAlive.keys.toList)
          // Informer tous les musiciens que le leader est hors service
          terminaux.foreach { t =>
            val cleanIp = t.ip.replaceAll("\"", "")
            val address = s"akka.tcp://MozartSystem${t.id}@${cleanIp}:${t.port}/user/Musicien${t.id}"
            context.actorSelection(address) ! Leader_out
          }
        }
      }


      println(s"Musicians alive: $musiciansAlive")
    }

    context.system.scheduler.scheduleOnce(checkInterval, self, CheckerTick)(context.dispatcher)
  }

  private def sendNewMusician(id: Int): Unit = {
    val terminal = terminaux.find(t => t.id == id)
    terminaux.foreach { t =>
      if (t.id != id) {
        val cleanIp = t.ip.replaceAll("\"", "")
        // Change theplayer to player
        val address = s"akka.tcp://MozartSystem${t.id}@${cleanIp}:${t.port}/user/Musicien${t.id}"
        val remote = context.actorSelection(address)
        remote ! New_Musician(terminal.get)
      }
    }
  }

}

