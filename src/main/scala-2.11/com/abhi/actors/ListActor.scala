package com.abhi.actors

import akka.actor.{Actor, ActorRef, Terminated}
import com.abhi.models.MyList
import com.abhi.messages._
import com.abhi.messages.IncrementAndGetId
import io.getquill.{CassandraAsyncContext, SnakeCase}
import com.abhi.serialization.Serialization._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by ASrivastava on 3/3/17.
  */
class ListActor(idTrackerActor: ActorRef) extends Actor {
   lazy val db = new CassandraAsyncContext[SnakeCase]("db") with ListEncoder with ListDecoder
   import db._
   def receive = {
      case InsertList(l) => idTrackerActor ! IncrementAndGetId("my_list", l, sender)
      case ListWithId(id, list, original) =>
         Await.result(db.run(insert(MyList(id, list))), Duration.Inf)
         original ! MyList(id, list)
      case Terminated(_) => db.close
      case t => println(s"don't know how to handle ${t.getClass.getName}")
   }
   def insert(list : MyList) = quote {
      query[MyList].insert(lift(list))
   }
}
