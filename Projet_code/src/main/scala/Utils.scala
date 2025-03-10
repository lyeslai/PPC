package upmc.akka.leader

// Général
case object Start
case object CheckAlive
case object CheckMusicians
case object Ping
case class Pong(terminal: Terminal)
case object ElectNewConductor

// Acteurs spécifiques
case class Terminal(id: Int, ip: String, port: Int)
case class MusicianFailed(musicianId: Int)

// Musique
case class Play_Measure(measure: DataBaseActor.Measure)
