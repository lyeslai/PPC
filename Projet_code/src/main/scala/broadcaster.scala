package upmc.akka.leader

import akka.actor._
import upmc.akka.leader.DataBaseActor.Measure

import scala.util.Random


trait Broadcast {
  this: Actor =>
  def terminaux: List[Terminal]


  def broadcastLeader(leader: Int): Unit = {
    terminaux.foreach { terminal =>
      val cleanIp = terminal.ip.replaceAll("\"", "")
      val address = s"akka.tcp://MozartSystem${terminal.id}@${cleanIp}:${terminal.port}/user/Musicien${terminal.id}"
      val remote = context.actorSelection(address)
      println("Broadcasting leader to " + terminal.id)
      remote ! Report_election_result(leader)
    }
  }


  def broadcastPresence(id: Int, isLeader: Boolean): Unit = {
    terminaux.foreach { t =>
      if (t.id != id) {
        val cleanIp = t.ip.replaceAll("\"", "")
        val address = s"akka.tcp://MozartSystem${t.id}@${cleanIp}:${t.port}/user/DeadCollector"
        val remote = context.actorSelection(address)
        remote ! Report_presence(id, isLeader)
      }
    }
  }


  // In broadcaster.scala - Fix the path
  def broadcastPlayer(message: Measure, players: List[Int]): Unit = {
    if (players.nonEmpty) {
      val randomId = players(Random.nextInt(players.size))
      terminaux.find(_.id == randomId) match {
        case Some(terminal) =>
          val cleanIp = terminal.ip.replaceAll("\"", "")
          // Change theplayer to player - matches the name in Musicien class
          val address = s"akka.tcp://MozartSystem${terminal.id}@${cleanIp}:${terminal.port}/user/Musicien${terminal.id}/player"
          val remote = context.actorSelection(address)
          remote ! message
          println(s"Send Measure to (id=${terminal.id}).")
        case None =>
          println(s"Aucun terminal trouvé pour l'id $randomId.")
      }
    }
  }
}