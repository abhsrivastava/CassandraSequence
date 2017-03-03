name := "untitled"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
   "io.getquill" % "quill-core_2.11" % "1.1.0",
   "io.getquill" % "quill-cassandra_2.11" % "1.1.0",
   "io.getquill" % "quill-async_2.11" % "1.1.0",
   "com.typesafe.akka" % "akka-actor_2.11" % "2.4.17"
)