package mozart.models

sealed trait MusicalObject

case class Note(pitch: Int, duration: Int, volume: Int) extends MusicalObject

case class Chord(time: Int, notes: List[Note]) extends MusicalObject

case class Measure(chords: List[Chord]) extends MusicalObject

case class GetMeasure (num:Int) extends MusicalObject

case class  RequestMeasure(num : Int) extends MusicalObject



/** For MIDI playback */
case class MidiNote(pitch: Int, velocity: Int, duration: Int, startTime: Int)
// Messages for the musicianActor
case object Initialize
case class ConductorInfo(id: Int)
case object ElectConductor
case object ConductorElected
case object PerformMusic

case object Shutdown


case class Terminal(id: Int, ip: String, port: Int)
