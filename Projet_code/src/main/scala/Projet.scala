package mozart

import actors.MusicianActor
import akka.actor._
import com.typesafe.config.ConfigFactory
import mozart.models._

/**
 * Main application entry point for the Mozart Distributed Game
 */
object MozartApp {
  def main(args: Array[String]): Unit = {
    // Validate command line arguments
    if (args.length != 1) {
      println("Usage: run <musician_id>")
      sys.exit(1)
    }

    val musicianId = args(0).toInt

    if (musicianId < 0 || musicianId > 3) {
      println("Error: musician_id must be between 0 and 3")
      sys.exit(1)
    }

    // Build the list of all nodes in the system
    var nodes = List.empty[Terminal]

    for (i <- 3 to 0 by -1) {
      val config = ConfigFactory.load().getConfig(s"system$i")
      val host = config.getString("akka.remote.netty.tcp.hostname")
      val port = config.getInt("akka.remote.netty.tcp.port")
      nodes = Terminal(i, host, port) :: nodes
    }

    println(s"Nodes configuration: $nodes")

    // Initialize the Akka system for this musician
    val system = ActorSystem(s"MozartSystem$musicianId",
      ConfigFactory.load().getConfig(s"system$musicianId"))

    val musician = system.actorOf(Props(new MusicianActor(musicianId, nodes)), s"musician$musicianId")

    // Start the musician
    musician ! Initialize
  }
}
