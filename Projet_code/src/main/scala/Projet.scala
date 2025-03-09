package upmc.akka.leader

import com.typesafe.config.ConfigFactory
import akka.actor._

case class Terminal (id:Int, ip:String, port:Int)

object Projet {

   
     def main (args : Array[String]) {
          // Gestion des erreurs
          if (args.size != 1) {
               println ("Erreur de syntaxe : run <num>")
               sys.exit(1)
          }

          val id : Int = args(0).toInt

          if (id < 0 || id > 3) {
               println ("Erreur : <num> doit etre compris entre 0 et 3")
               sys.exit(1)
          }

          var musicienlist = List[Terminal]()
          
          // recuperation des adresses de tous les musiciens
          // hardcoded path name
          for(i <- 3 to 0 by -1){
               val address = ConfigFactory.load().getConfig("system"+ i).getValue("akka.remote.netty.tcp.hostname").render()
               val port = ConfigFactory.load().getConfig("system"+ i).getValue("akka.remote.netty.tcp.port").render()
               musicienlist = Terminal(i, address, port.toInt)::musicienlist
          }

          println(musicienlist)

          // Initialisation du node <id>
          val system = ActorSystem("MozartSystem" + id, ConfigFactory.load().getConfig("system" + id))
          val electionActor = system.actorOf(Props(new ElectionActor(musicienlist)), "ElectionActor")
          val deadCollector = system.actorOf(Props(new DeadCollector(musicienlist, electionActor)), "DeadCollector")
          val musicien = system.actorOf(Props(new Musicien(id, musicienlist,deadCollector)), "Musicien"+id)
          electionActor ! Start
          deadCollector ! Start
          musicien ! Start
     }

}
