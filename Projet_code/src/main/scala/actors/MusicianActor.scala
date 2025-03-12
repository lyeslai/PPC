package actors

import akka.actor._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import mozart.models._

class MusicianActor(val id: Int, val nodes: List[Terminal]) extends Actor {
  // Child actors
  private val console = context.actorOf(Props[DisplayActor], "console")
  private val dataBase = context.actorOf(Props[DataBaseActor], "dataBase")
  private val player = context.actorOf(Props[PlayerActor], "player")
  private val pulse = context.actorOf(Props(new PulseActor(id)), "pulse")
  private val monitor = context.actorOf(Props(new MonitorActor(id, pulse)), "monitor") // prestart of monitor schedules regular status checks
  private val scoreProvider = context.actorOf(Props(new ScoreProviderActor(self)), "scoreProvider")

  // Random generator for dice rolling
  private val random = new scala.util.Random

  // Musician state
  private var musicianStatus = Array.fill[Int](4)(-1)
  private var otherMusiciansAlive = 0
  private var isAlone = false
  private var conductorId = -1
  private var absentMusiciansCount = 0
  private var currentNote: Measure = null

  // Initialize this musician as available
  musicianStatus(id) = 0

  // Get actor references to all other musicians
  private val otherMusicians = Array.tabulate[ActorSelection](4) { i =>
    if (i != id) {
      context.actorSelection(s"akka.tcp://MozartSystem$i@127.0.0.1:600$i/user/musician$i")
    } else null
  }

  /**
   * Roll dice to select a measure
   * @return A dice roll result (2-12)
   */
  private def rollDice(): Int = {
    val d1 = random.nextInt(6) + 1
    val d2 = random.nextInt(6) + 1
    d1 + d2
  }

  def receive: Receive = {
    case Initialize =>
      console ! Message(s"Musician $id initialized")

    // Handle status updates from monitor
    case MusicianStatusUpdate(targetId, status) =>
      console ! Message(s"Musician $targetId status updated to $status")
      val previousStatus = musicianStatus(targetId)
      musicianStatus(targetId) = status

      status match {
        case 0 if previousStatus == -1 =>
          // Musician came back online
          if (otherMusiciansAlive == 0 && conductorId == id) self ! PerformMusic
          isAlone = false
          otherMusiciansAlive += 1
          if (conductorId != -1) otherMusicians(targetId) ! ConductorInfo(conductorId)

        case -1 if previousStatus == 0 =>
          // Musician went offline
          otherMusiciansAlive -= 1

        case -1 if previousStatus == 1 =>
          // Conductor went offline
          otherMusiciansAlive -= 1
          self ! ElectConductor

        case 1 if previousStatus == -1 =>
          // New conductor appeared
          otherMusiciansAlive += 1
          conductorId = targetId

        case -1 if previousStatus == -1 && conductorId == -1 =>
          // Continuous absence - might need to elect a conductor
          absentMusiciansCount += 1
          if (absentMusiciansCount == 4) {
            self ! ElectConductor
          }

        case _ =>
          // Catch-all case for any unhandled status combinations
      }
    // Handle conductor information
    case ConductorInfo(conductorId) =>
      if (conductorId != this.id && musicianStatus(conductorId) == 0) {
        this.conductorId = conductorId
      }

    // Elect a new conductor
    case ElectConductor =>
      var conductorElected = false
      var i = 3

      // Try to elect the musician with highest ID that's online
      while (i >= 0 && !conductorElected) {
        if (musicianStatus(i) == 0) {
          conductorElected = true
          if (i == id) {
            self ! ConductorElected
            pulse ! SetAsConductor
          } else {
            conductorId = i
          }
        }
        i -= 1
      }

    // Handle conductor election
    case ConductorElected =>
      if (conductorId != id) {
        conductorId = id
        console ! Message("I am now the conductor! I will begin the performance.")
        self ! PerformMusic
      }

    // Perform music as conductor
    case PerformMusic =>
      if (conductorId == id) {
        if (otherMusiciansAlive > 0) {
          // Select a random musician to play
          var targetMusician = -1
          var randIndex = random.nextInt(otherMusiciansAlive) + 1

          for (i <- 0 until 4) {
            if (i != id && musicianStatus(i) == 0) {
              randIndex -= 1
              if (randIndex == 0) targetMusician = i
            }
          }

          console ! Message(s"Assigning musician $targetMusician to play")

          // Get a measure based on dice roll
          val diceRoll = rollDice()
          scoreProvider ! RequestScore(diceRoll)

          // Schedule next performance
          context.system.scheduler.scheduleOnce(1800.milliseconds, self, PerformMusic)
        } else {
          isAlone = true
          console ! Message("No other musicians available. Waiting 30 seconds before shutting down...")
          context.system.scheduler.scheduleOnce(30.seconds, self, Shutdown)
        }
      }

    // Handle shutdown
    case Shutdown =>
      if (isAlone) {
        console ! Message("No musicians joined. Shutting down...")
        context.system.terminate()
        System.exit(0)
      }

    // Handle measure requests (from  provider)
    case RequestMeasure(num) =>
      dataBase ! GetMeasure(num)

    // Handle measures (from db or other musicians)
    case measure: Measure =>
      if (conductorId == id) {
        // If I'm the conductor, assign to another musician
        if (otherMusiciansAlive > 0) {
          var targetMusician = -1
          var randIndex = random.nextInt(otherMusiciansAlive) + 1

          for (i <- 0 until 4) {
            if (i != id && musicianStatus(i) == 0) {
              randIndex -= 1
              if (randIndex == 0) targetMusician = i
            }
          }

          otherMusicians(targetMusician) ! measure
        }
      } else {
        // If I'm not the conductor, play the measure
        console ! Message(s"Playing measure with ${measure.chords.size} chords")
        currentNote = measure
        player ! measure
      }

  }
}
