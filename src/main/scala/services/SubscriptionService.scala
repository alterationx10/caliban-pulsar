package services

import domain.ResponseEvent
import domain.ResponseEvent._
import org.apache.pulsar.client.api.Consumer
import zio._
import zio.stream.ZStream
import zio.json._

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

  private val acquire: ZIO[Has[Consumer[String]], Throwable, Hub[ResponseEvent]] = for {
    hub <- Hub.unbounded[ResponseEvent]
    _   <- ZIO.serviceWith[Consumer[String]] { consumer =>
      Task(consumer.receive())
        .tap(msg => Task(consumer.acknowledge(msg)))
        .map(msg => new String(msg.getData))
        .flatMap(str => ZIO.fromEither(str.fromJson))
        .mapError(err => new Exception(err.toString))
        .flatMap(event => hub.publish(event))
        .forever
        .forkDaemon
    }
  } yield hub

  val live: ZLayer[Has[Consumer[String]], Throwable, Has[Hub[ResponseEvent]]] =
    ZLayer.fromAcquireRelease(acquire)(hub => hub.shutdown)
}
