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
      println(s"Broadcasting leader ($leader) to musician ${terminal.id}")
      remote ! Report_election_result(leader)
    }
  }



  def broadcastPresence(id: Int, isLeader: Boolean): Unit = {
    terminaux.foreach { t =>
      if (t.id != id) {
        val cleanIp = t.ip.replaceAll("\"", "")
        // user/Musicien${t.id}/DeadCollector
        val address = s"akka.tcp://MozartSystem${t.id}@${cleanIp}:${t.port}/user/Musicien${t.id}/DeadCollector"
        val remote = context.actorSelection(address)
        remote ! Report_presence(id, isLeader)
      }
    }
  }



}