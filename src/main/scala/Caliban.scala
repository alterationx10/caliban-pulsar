import caliban.ZHttpAdapter
import org.apache.pulsar.client.api.{Consumer, Producer, PulsarClient}
import repository.UserEventRepository
import services.{
  PulsarClientConfig,
  PulsarClientProvider,
  PulsarConsumerConfig,
  PulsarConsumerProvider,
  PulsarProducerConfig,
  PulsarProducerProvider,
  SubscriptionService
}
import zhttp.http._
import zhttp.service.Server
import zio._

import java.util.UUID

object Caliban extends App {

  val host: String = sys.env.getOrElse("PULSAR_HOST", "localhost")

  val client: ZLayer[Any, Throwable, Has[PulsarClient]] = ZLayer.succeed(
    PulsarClientConfig(serviceUrl = s"pulsar://$host:6650")
  ) >>> PulsarClientProvider.layer

  val producer: ZLayer[Any, Throwable, Has[Producer[String]]] =
    (client ++ ZLayer.succeed(
      PulsarProducerConfig(topic = "backend")
    )) >>> PulsarProducerProvider.layer

  val consumer: ZLayer[Any, Throwable, Has[Consumer[String]]] =
    (client ++ ZLayer.succeed(
      PulsarConsumerConfig(
        topics = List("caliban"),
        subscription = s"front-${UUID.randomUUID().toString}"
      )
    )) >>> PulsarConsumerProvider.layer

  val subservice: ZLayer[Any, Throwable, Has[SubscriptionService]] =
    consumer >>> SubscriptionService.live

  val deps: ZLayer[Any, Throwable, Has[UserEventRepository] with Has[
    Producer[String]
  ] with Has[Consumer[String]] with Has[SubscriptionService]] =
    UserEventRepository.live ++ producer ++ consumer ++ subservice

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (for {
      interpreter <- Api.api.interpreter
      _           <- Server
        .start(
          8088,
          Http.route {
            case _ -> Root / "api" / "graphql" =>
              ZHttpAdapter.makeHttpService(interpreter)
            case _ -> Root / "ws" / "graphql"  =>
              ZHttpAdapter.makeWebSocketService(interpreter)
          }
        )
        .forever
    } yield ())
      .provideCustomLayer(ZEnv.live ++ deps)
      .exitCode
}
