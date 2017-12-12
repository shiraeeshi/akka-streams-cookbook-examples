package com.example

import akka.stream._
import akka.stream.scaladsl._

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

object MainHundred extends App {

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)
  val done: Future[Done] = source.runForeach(i => println(i))

  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())
}

object MainFactorialsToFile extends App {

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)
  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  val path = Paths.get("factorials.txt")
  println(s"path: $path")
  
  val result: Future[IOResult] =
    factorials
      .map(num => ByteString(s"$num\n"))
      .runWith(FileIO.toPath(path))

  implicit val ec = system.dispatcher
  result.onComplete(_ => system.terminate())
}

object MainFactorialsToFileUsingSink extends App {

  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s => ByteString("|" + s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)
  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  
  val result: Future[IOResult] =
    factorials
      .map(_.toString)
      .runWith(lineSink("factorials2.txt"))

  implicit val ec = system.dispatcher
  result.onComplete(_ => system.terminate())
}

object MainFactorialsUsingThrottle extends App {

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)
  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  val done: Future[Done] =
    factorials
      .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
      .throttle(1, 300.milliseconds, 1, ThrottleMode.shaping)
      .runForeach(println)

  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())
}
