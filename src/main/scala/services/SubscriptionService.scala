package services

import domain.ResponseEvent
import domain.ResponseEvent._
import org.apache.pulsar.client.api.Consumer
import zio._
import zio.stream.ZStream
import zio.json._
import zio.console._

trait SubscriptionService {
  def events(id: String): ZStream[Any, Throwable, ResponseEvent]
}

case class SubscriptionServiceLive(hub: Hub[ResponseEvent]) extends SubscriptionService {

  override def events(id: String): ZStream[Any, Throwable, ResponseEvent] =
    ZStream.unwrapManaged(
      hub.subscribe.map(
        ZStream
          .fromQueue(_)
          .filter(_.id == id)
      )
    )
}

object SubscriptionService {

  val live: ZLayer[Has[Consumer[String]], Throwable, Has[SubscriptionService]] =
    SubHub.live >>> (SubscriptionServiceLive(_)).toLayer

  def events(
    id: String
  ): ZStream[Has[SubscriptionService], Throwable, ResponseEvent] =
    ZStream.accessStream(_.get.events(id))
}

object SubHub {

  private def consumerLogic(consumer: Consumer[String], hub: Hub[ResponseEvent]): ZIO[Console, Throwable, Unit] = for {
    msg   <- Task(consumer.receive())
    _     <- Task(consumer.acknowledge(msg)) // auto ack
    str   <- Task(new String(msg.getData))
    _     <- putStrLn(s" Received: $msg")
    model <- ZIO.fromEither(str.fromJson[ResponseEvent]).mapError(err => new Exception(err))
    _     <- hub.publish(model)
  } yield ()

  private val acquire: ZIO[Has[Consumer[String]], Throwable, Hub[ResponseEvent]] = for {
    hub      <- Hub.unbounded[ResponseEvent]
    consumer <- ZIO.service[Consumer[String]]
    _        <- consumerLogic(consumer, hub)
      .provideLayer(Console.live)
      .forever
      .forkDaemon
  } yield hub

  val live: ZLayer[Has[Consumer[String]], Throwable, Has[Hub[ResponseEvent]]] =
    ZLayer.fromAcquireRelease(acquire)(hub => hub.shutdown)
}
