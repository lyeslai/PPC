package upmc.akka.leader

import com.typesafe.config.ConfigFactory
import akka.actor._

object Projet {
  def main(args: Array[String]) {
    if (args.length != 1 || !args(0).forall(_.isDigit)) {
      println("Erreur de syntaxe : run <num>")
      sys.exit(1)
    }

    val id = args(0).toInt
    val terminaux = (0 to 3).map { i =>
      val conf = ConfigFactory.load().getConfig(s"system$i")
      Terminal(i, conf.getString("akka.remote.netty.tcp.hostname"), conf.getInt("akka.remote.netty.tcp.port"))
    }.toList

    println(terminaux)

    val system = ActorSystem(s"MozartSystem$id", ConfigFactory.load().getConfig(s"system$id"))
    val musicien = system.actorOf(Props(new Musicien(id, terminaux)), s"Musicien$id")
    val deadCollector = system.actorOf(Props(new DeadCollector(terminaux)), s"DeadCollector$id")

    musicien ! Start
  }
}