package com.abhi.serialization

import io.getquill.context.cassandra.CassandraSessionContext

import scala.reflect.ClassTag
import scala.collection.JavaConverters._

object Serialization {

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
}