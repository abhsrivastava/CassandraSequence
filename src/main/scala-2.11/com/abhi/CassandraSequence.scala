package com.abhi

/**
  * Created by ASrivastava on 3/2/17.
  */
import io.getquill._
import io.getquill.context.cassandra.CassandraSessionContext

import scala.collection._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.collection.JavaConverters._
import akka.actor._
import akka.pattern.ask
import com.abhi.actors.{ListActor, ListIdTrackerActor}
import com.abhi.messages.{InitializeTracker, InsertList}
import com.abhi.models.{ListIdTracker, MyList}

object CassandraSequence extends App {
   implicit val timeout = akka.util.Timeout(5 seconds)
   val system = ActorSystem("list")
   val idTracker = system.actorOf(Props[ListIdTrackerActor], name="listidtracker")
   idTracker ! InitializeTracker()
   val listActor = system.actorOf(Props(new ListActor(idTracker)), name="listActor")
   val result = for {
      id1 <- listActor ? InsertList(List("1", "2"))
      id2 <- listActor ? InsertList(List("3", "4"))
      id3 <- listActor ? InsertList(List("5", "6"))
      id4 <- listActor ? InsertList(List("7", "8"))
   } yield List(id1, id2, id3, id4)
   val list = Await.result(result, Duration.Inf).map(_.asInstanceOf[MyList])
   list.foreach{ l => println(l.id)}
   scala.io.StdIn.readLine()
   system.terminate()
   println("done")
}