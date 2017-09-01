import akka.actor.ActorSystem

package object globals {
  implicit val actorSystem = ActorSystem("account-service")
}
