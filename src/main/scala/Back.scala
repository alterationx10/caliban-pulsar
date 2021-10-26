import domain.{ResponseEvent, UserEvent}
import org.apache.pulsar.client.api.{Consumer, Producer, PulsarClient}
import repository.{RedisProvider, UserEventRepository}
import services.{PulsarClientConfig, PulsarClientProvider, PulsarConsumerConfig, PulsarConsumerProvider, PulsarProducerConfig, PulsarProducerProvider}
import zio._
import zio.console.putStrLn

import java.util.UUID
import zio.json._

object Back extends App {

  val client: ZLayer[Any, Throwable, Has[PulsarClient]] = ZLayer.succeed(PulsarClientConfig(serviceUrl = "pulsar://localhost:6650")) >>> PulsarClientProvider.layer
  val producer: ZLayer[Any, Throwable, Has[Producer[String]]] = (client ++ ZLayer.succeed(PulsarProducerConfig(topic = "caliban"))) >>> PulsarProducerProvider.layer
  val consumer: ZLayer[Any, Throwable, Has[Consumer[String]]] = (client ++ ZLayer.succeed(PulsarConsumerConfig(topics = List("back"), subscription = s"back-shared"))) >>> PulsarConsumerProvider.layer

  val deps =
    ZEnv.live ++ UserEventRepository.live ++ producer ++ consumer ++ RedisProvider.redisPool

  val program = for {
    _ <- putStrLn("Starting Back...")
    producer <- ZIO.service[Producer[String]]
    _ <- ZIO.serviceWith[Consumer[String]](c => Task(c.receive())).flatMap(msg => Task(new String(msg.getData)))
      .tap(ev => putStrLn(s"Received $ev"))
      .flatMap(msg => ZIO.fromEither(msg.fromJson[UserEvent]).mapError(msg => new Exception(msg)))
      .flatMap(ev => RedisProvider.managedRedis.use { redis =>
        Task(redis.set(ev.id, ev.toJson)).map(status => ResponseEvent(id = ev.id, status = status))
      })
      .flatMap(msg => Task(producer.send(msg.toJson)))
      .forever
  } yield ExitCode.success

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = program.provideLayer(deps).exitCode
}
