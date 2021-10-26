import repository.UserEventRepository
import zio._
import zhttp.http._
import zhttp.service.Server
import caliban.ZHttpAdapter
import org.apache.pulsar.client.api.{Consumer, Producer, PulsarClient}
import services.{PulsarClientConfig, PulsarClientProvider, PulsarConsumerConfig, PulsarConsumerProvider, PulsarProducerConfig, PulsarProducerProvider}
import zio.console._

import java.util.UUID

object Caliban extends App {

  val client: ZLayer[Any, Throwable, Has[PulsarClient]] = ZLayer.succeed(PulsarClientConfig(serviceUrl = "pulsar://localhost:6650"))  >>> PulsarClientProvider.layer
  val producer: ZLayer[Any, Throwable, Has[Producer[String]]] =  (client ++ ZLayer.succeed(PulsarProducerConfig(topic = "back"))) >>> PulsarProducerProvider.layer
  val consumer: ZLayer[Any, Throwable, Has[Consumer[String]]] = (client ++ ZLayer.succeed(PulsarConsumerConfig(topics = List("caliban"), subscription = s"front-${UUID.randomUUID().toString}"))) >>> PulsarConsumerProvider.layer

  val deps =
    UserEventRepository.live ++ producer ++ consumer



  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (for {
      interpreter <- Api.api.interpreter
      _           <- Server
        .start(
          8088,
          Http.route {
            case _ -> Root / "api" / "graphql" => ZHttpAdapter.makeHttpService(interpreter)
            case _ -> Root / "ws" / "graphql"  => ZHttpAdapter.makeWebSocketService(interpreter)
          }
        )
        .forever
    } yield ())
      .provideCustomLayer(ZEnv.live ++ deps)
      .exitCode
}
