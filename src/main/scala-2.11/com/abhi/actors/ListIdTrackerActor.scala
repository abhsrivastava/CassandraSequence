package com.abhi.actors

import akka.actor.Actor
import com.abhi.models.ListIdTracker
import com.abhi.messages._
import com.abhi.messages.{ListWithId, StoreListId}
import io.getquill.{CassandraAsyncContext, SnakeCase}
import com.abhi.serialization.Serialization._
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by ASrivastava on 3/3/17.
  */
class ListIdTrackerActor extends Actor {
   lazy val db = new CassandraAsyncContext[SnakeCase]("db") with ListEncoder with ListDecoder
   import db._
   var map = mutable.Map[String, Int]()
   def receive = {
      case req : InitializeTracker =>
         val list = Await.result(db.run(getAllIds), Duration.Inf)
         list.foreach { l =>
            map(l.listName) = l.id
         }
      case req : IncrementAndGetId =>
         increment(req.listName, req.list)
         sender ! ListWithId(map(req.listName), req.list, req.original)
      case req : StoreListId =>
         Await.result(db.run(insert(ListIdTracker(req.list_name, req.id))), Duration.Inf)
      case t => println(s"don't know what to do with ${t.getClass.getName}")
   }
   def increment(listName: String, list: List[String]) = {
      map(listName) = map.getOrElse(listName, 0) + 1
      self ! StoreListId(listName, map(listName))
   }
   def insert(req: ListIdTracker) = quote {
      query[ListIdTracker].insert(lift(req))
   }
   def getAllIds() = quote {
      query[ListIdTracker]
   }
}
