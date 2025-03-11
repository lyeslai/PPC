package upmc.akka.leader

// Général
case object CheckAlive
case object CheckMusicians
case object EndMeasure
case class Pong(terminal: Terminal)
case object StopP
case class reportCurrentAlive(aliveList: List[Int])

// Acteurs spécifiques
case class MusicianFailed(musicianId: Int)

// Musique
