package com.example.cookbook

import akka.stream._
import akka.stream.scaladsl._

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._

object Domain {
  case class Message(body: String)
  case class Topic(title: String)

  val topics = List(
    "sports-1",
    "economics-1",
    "economics-2",
    "science-1",
    "science-2",
    "science-3",
    "politics-1",
    "politics-2",
    "politics-3",
    "politics-4",
    "education-1",
    "education-2",
    "education-3",
    "education-4",
    "education-5"
  ).map(Topic(_))

  val messages = List(
    "sports-msg-1",
    "sports-msg-2",
    "sports-msg-3",
    "sports-msg-4",
    "sports-msg-5",
    "economics-msg-1",
    "economics-msg-2",
    "economics-msg-3",
    "economics-msg-4",
    "science-msg-1",
    "science-msg-2",
    "science-msg-3",
    "politics-msg-1",
    "politics-msg-2",
    "education-msg-1"
  ).map(Message(_))
}

object ThisApp extends App {
  import Domain._

  implicit val system = ActorSystem("reactive-tweets")
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  def topicMapper(msg: Message): List[Topic] =
    topics.filter(topic => msg.body.startsWith(topic.title.takeWhile(_ != '-')))

  val elems: Source[Message, NotUsed] = Source(messages)

  val messageAndTopic: Source[(Message, Topic), NotUsed] =
    elems.mapConcat { msg: Message =>
      val topicsForMessage = topicMapper(msg)
      topicsForMessage.map(msg -> _)
    }

  val multiGroups = messageAndTopic
    .groupBy(15, _._1)
//    .map { case (msg, topic) =>
//      // TODO do what needs to be done
//      println(s"msg: $msg")
//      println(s"topic: $topic")
//      (msg, topic)
//    }
  
  //val f = multiGroups.runForeach(println)

  //val f = multiGroups.to(Sink.foreach(println)).run()
  //val f = multiGroups.map(t => println(t)).to(Sink.ignore).run()
  
  //val f = messageAndTopic.runForeach(println)
  //f onComplete { _ => system.terminate() }
  
  //f onComplete { seq =>
  //  println(s"seq: $seq")
  //  system.terminate()
  //}

  val zero = (None, Nil)
  val resultSource = multiGroups// map { case t@(msg, topic) =>
    //println(s"msg: $msg, topic: $topic")
    //t
  //}
  .scan[(Option[Message], List[Topic])](zero) {
    case (opt, (msg, topic)) if opt == zero =>
      (Some(msg), topic :: Nil)
    case ((_, topics), (msg, topic)) =>
      (Some(msg), topic :: topics)
  } map { case t@(msg, topics) =>
    println(s"msg: $msg, topics: $topics")
    t
  }

  val f = resultSource.to(Sink.ignore).run()
  //f onComplete { _ => system.terminate() }
}
