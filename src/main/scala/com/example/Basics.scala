package com.example

import akka.stream._
import akka.stream.scaladsl._

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

object BasicsSumOneToTen extends App {

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val source = Source(1 to 10)
  val sink = Sink.fold[Int, Int](0)(_ + _)

  val runnable: RunnableGraph[Future[Int]] =
    source.toMat(sink)(Keep.right)

  val futureSum: Future[Int] = runnable.run()

  implicit val ec = system.dispatcher

  futureSum onComplete { sum =>
    println(s">>> sum: $sum")
    system.terminate()
  }
}

object BasicsSumOneToTenUsingRunWith extends App {

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val source = Source(1 to 10)
  val sink = Sink.fold[Int, Int](0)(_ + _)

  val futureSum: Future[Int] =
    source.runWith(sink)

  implicit val ec = system.dispatcher

  futureSum onComplete { sum =>
    println(s">>> sum: $sum")
    system.terminate()
  }
}

object BasicsSumOneToTenRunTwice extends App {

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val source = Source(1 to 10)
  val sink = Sink.fold[Int, Int](0)(_ + _)

  val runnable: RunnableGraph[Future[Int]] =
    source.toMat(sink)(Keep.right)

  val futureSum1: Future[Int] = runnable.run()
  val futureSum2: Future[Int] = runnable.run()

  implicit val ec = system.dispatcher

  for {
    sum1 <- futureSum1
    sum2 <- futureSum2
  } {
    println(s">>> sum1: $sum1, sum2: $sum2")
    system.terminate()
  }
}
