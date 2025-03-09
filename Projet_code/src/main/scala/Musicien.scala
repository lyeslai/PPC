package upmc.akka.leader

import akka.actor._

import scala.concurrent.duration.DurationInt

sealed trait MessageMusician
case class Start() extends MessageMusician
case class Report_presence(id: Int, isLeader: Boolean) extends MessageMusician
case object Check_leader extends MessageMusician
case class Report_election_result(id: Int) extends MessageMusician
case object Check_end extends MessageMusician
case object Play_Music extends MessageMusician
case object Leader_out extends MessageMusician
case class Leader_found(id: Int) extends MessageMusician
case class New_Musician(terminal: Terminal) extends MessageMusician


class Musicien (val id:Int, var terminaux:List[Terminal], deadCollector: ActorRef) extends Actor
  with Broadcast {
     // Les differents acteurs du systeme
     val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor")
     val chefOrchestre = context.actorOf(Props(new ChefOrchestre(id, terminaux)), name = "chefOrchestre")
     val playerActor = context.actorOf(Props[PlayerActor], "player")
     val reporterActor = context.actorOf(Props[ReporterActor], name = "reporterActor")


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
              // do a leader check right away
              context.system.scheduler.scheduleOnce(1.seconds, self, Check_leader)(context.dispatcher)
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
          case Report_election_result(id) => {
                if(id == this.id){
                      this.isLeader = true
                      displayActor ! Message("Musicien " + this.id + " is the leader")
                      println("Musicien " + this.id + " is the leader")
                     // act as leader
                     chefOrchestre ! Start
                }
               this.leader = id
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

          case Play_Music => {
              isPlaying = true
              displayActor.tell(Message("Musicien " + this.id + " is playing"), self)
              playerActor ! Start
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
               terminaux = terminal::terminaux
          }


     }


}




