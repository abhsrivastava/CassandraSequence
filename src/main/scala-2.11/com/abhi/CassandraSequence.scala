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
      case req : Increment => increment(req.listName, req.list)
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

// database classes
case class MyList(id : Int, listData: List[String])
case class ListIdTracker(listName: String, id: Int)

// messages
case class InitializeTracker()
case class StoreListId(list_name: String, id: Int)
case class ListWithId(id: Int, list_data : List[String], original: ActorRef)
case class InsertList(list: List[String])
case class IncrementAndGetId(listName: String, list: List[String], original : ActorRef)
case class Increment(listName: String, list: List[String])


trait ListEncoder {
   this: CassandraSessionContext[_] =>
   implicit def listEncoder[T](implicit t: ClassTag[T]) : Encoder[List[T]] = {
      encoder((index, value, row) => row.setList[T](index, value.asJava, t.runtimeClass.asInstanceOf[Class[T]]))
   }
}

trait ListDecoder {
   this: CassandraSessionContext[_] =>
   implicit def setDecoder[T](implicit t: ClassTag[T]) : Decoder[List[T]] = {
      decoder((index, row) => row.getList(index, t.runtimeClass.asInstanceOf[Class[T]]).asScala.toList)
   }
}