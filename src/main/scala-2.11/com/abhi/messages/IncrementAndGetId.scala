package com.abhi.messages

import akka.actor.ActorRef

/**
  * Created by ASrivastava on 3/3/17.
  */
case class IncrementAndGetId(listName: String, list: List[String], original : ActorRef)
