package com.example

import akka.stream._
import akka.stream.scaladsl._

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

object ReactiveStreamsDomain {
  final case class Author(handle: String)
  final case class Hashtag(name: String)
  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] = body.split(" ").collect {
      case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
    }.toSet
  }
  val akkaTag = Hashtag("#akka")
  val tweetsList = List(
    Tweet(Author("john hopkins"), 100000000, "#apples in #almaty"),
    Tweet(Author("mike johnson"), 100000001, "went to buy some #akka"),
    Tweet(Author("steve flanagan"), 100000002, "#akka rules"),
    Tweet(Author("helen simpson"), 100000003, "lalala"),
    Tweet(Author("ruth stone"), 100000004, "#banana"),
    Tweet(Author("michael jordan"), 100000005, "just do it #akka"),
  )
}

object ReactiveStreamsApp extends App {

  import ReactiveStreamsDomain._

  implicit val system = ActorSystem("reactive-tweets")
  implicit val materializer = ActorMaterializer()

  val tweets: Source[Tweet, NotUsed] =
    Source(tweetsList)
  val authors: Source[Author, NotUsed] =
    tweets
      .filter(_.hashtags.contains(akkaTag))
      .map(_.author)

  val hashtags = tweets.mapConcat(_.hashtags.toList)

  //authors.runForeach(println)
  val futureAuthorsDone: Future[Done] =
    authors.runWith(Sink.foreach(println))
  val futureHashtagsDone: Future[Done] =
    hashtags.runWith(Sink.foreach(println))

  implicit val ec = system.dispatcher

  for {
    _ <- futureAuthorsDone
    _ <- futureHashtagsDone
  } system.terminate()
}

object ReactiveStreamsGraph extends App {

  import ReactiveStreamsDomain._

  implicit val system = ActorSystem("reactive-tweets")
  implicit val materializer = ActorMaterializer()

  val tweets: Source[Tweet, NotUsed] =
    Source(tweetsList)

  implicit val ec = system.dispatcher
  
  val writeAuthors: Sink[Author, Future[Done]] = Sink.foreach(println)
  val writeHashtags: Sink[Hashtag, Future[Done]] = Sink.foreach(println)
  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val bcast = b.add(Broadcast[Tweet](2))
    tweets ~> bcast.in
    bcast.out(0) ~> Flow[Tweet].map(_.author) ~> writeAuthors
    bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> writeHashtags
    ClosedShape
  })

  g.run()
}

object ReactiveStreamsGraphMerge extends App {

  import ReactiveStreamsDomain._

  implicit val system = ActorSystem("reactive-tweets")
  implicit val materializer = ActorMaterializer()

  val tweets: Source[Tweet, NotUsed] =
    Source(tweetsList)

  implicit val ec = system.dispatcher
  
  val writeAuthors = Flow[Author].collect { case author =>
    println(author)
    author
  }
  val writeHashtags = Flow[Hashtag].collect { case hashtag =>
    println(hashtag)
    hashtag
  }
  val g: RunnableGraph[Future[Any]] = RunnableGraph.fromGraph(GraphDSL.create(Sink.head[Any]) { implicit b: GraphDSL.Builder[Future[Any]] => out =>
    import GraphDSL.Implicits._

    val bcast = b.add(Broadcast[Tweet](2))
    val merge = b.add(Merge[Any](2))

    //val out = Sink.head[Any]

    //tweets ~> bcast.in
    //bcast.out(0) ~> Flow[Tweet].map(_.author) ~> writeAuthors ~> merge ~> out
    //bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> writeHashtags
    tweets ~> bcast ~> Flow[Tweet].map(_.author)                ~> writeAuthors  ~> merge ~> out
              bcast ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> writeHashtags ~> merge
    ClosedShape
  })

  val result = g.run()
  result onComplete { mergedResult =>
    println(s"mergedResult: $mergedResult")
    system.terminate()
  }
}
