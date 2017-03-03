package com.abhi.messages

import akka.actor.ActorRef

/**
  * Created by ASrivastava on 3/3/17.
  */
case class ListWithId(id: Int, list_data : List[String], original: ActorRef)
