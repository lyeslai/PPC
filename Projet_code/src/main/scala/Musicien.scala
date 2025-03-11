package upmc.akka.leader

import akka.actor._
import upmc.akka.leader.DataBaseActor.Measure

import scala.concurrent.duration.DurationInt

sealed trait MessageMusician
case class Start() extends MessageMusician
case class Report_presence(id: Int, isLeader: Boolean) extends MessageMusician
case class Report_election_result(id: Int) extends MessageMusician
case class Play_Music(measure: Measure) extends MessageMusician
case object Check_leader extends MessageMusician
case object Check_end extends MessageMusician
case object Leader_out extends MessageMusician
case class Leader_found(id: Int) extends MessageMusician
case class New_Musician(terminal: Terminal) extends MessageMusician


class Musicien (val id:Int, var terminaux:List[Terminal], electionActor: ActorRef) extends Actor
  with Broadcast {
     // Les differents acteurs du systeme
     val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor")
     val chefOrchestre = context.actorOf(Props(new ChefOrchestre(id, terminaux)), name = "chefOrchestre")
     val playerActor = context.actorOf(Props[PlayerActor], "player")
     val reporterActor = context.actorOf(Props[ReporterActor], name = "reporterActor")
     val deadCollector = context.actorOf(Props(new DeadCollector(terminaux, electionActor)), "DeadCollector")

  var musiciansAlive : Map[Int, Boolean] = Map( this.id -> true)

  // variables to keep track of leader, if were playing or not and if we are dead
     var leader : Int = -1
     var isLeader: Boolean = false
     var isPlaying: Boolean = false



     // TODO : envoie message à actor MORT ( pour collecter si il est mort ou pas)


     def receive = {

          // Initialisation
          case Start => {
              displayActor ! Message ("Musicien " + this.id + " is created")
              reporterActor ! Start

              deadCollector ! Start
              // do a leader check right away
              context.system.scheduler.scheduleOnce(2.seconds, self, Check_leader)(context.dispatcher)
              context.system.scheduler.scheduleOnce(5.seconds, self, Presence_report)(context.dispatcher)
          }

          case Presence_report => {
//               broadcastPresence(Report_presence(this.id, this.isLeader))
            println("Presence report from " + this.id)
            deadCollector ! Report_presence(this.id, this.isLeader)
               if(this.isLeader){
                    context.system.scheduler.scheduleOnce(5.seconds, self, Check_end)(context.dispatcher)
               }
          }

         // elected leader result
          case Report_election_result(id) =>
            this.leader = id
            if (id == this.id) {
              this.isLeader = true
              displayActor ! Message(s"Musicien $id est le leader")
              println(s"Musicien $id est le leader")
              // Seul le leader lance son chef d'orchestre avec la liste des musiciens vivants
              val aliveList: List[Terminal] = terminaux.filter(t => musiciansAlive.getOrElse(t.id, false))
              chefOrchestre ! StartOnlyLive(aliveList)
            } else {
              // Les non-leaders n'initient pas le lancement
              this.isLeader = false
            }



          // check if leader is still alive
          case Check_leader => {
            println("Checking leader " + this.leader)
               if(this.leader != -1){
                 println("asking if leader is alive")
                    deadCollector ! Check_leader
               }
               context.system.scheduler.scheduleOnce(15.seconds, self, Check_leader)(context.dispatcher)
          }

          case Play_Music(measure) => {
              isPlaying = true
              displayActor.tell(Message("Musicien " + this.id + " is playing"), self)
              playerActor ! measure
          }

          case Leader_out => {
                this.leader = -1
                displayActor ! Message ("Leader is dead")
                println("Leader is dead")
          }

          case Leader_found (id) => {
               this.leader = id
               displayActor ! Message ("Leader found, it's " + id)
          }

          case New_Musician(terminal) => {
            displayActor ! Message("New musician added")
            musiciansAlive += (terminal.id -> true)
          }


     }


}




