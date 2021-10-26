import domain.{ResponseEvent, UserEvent}
import org.apache.pulsar.client.api.{Consumer, Producer, PulsarClient, SubscriptionType}
import redis.clients.jedis.JedisPool
import repository.{RedisProvider, UserEventRepository}
import services.{PulsarClientConfig, PulsarClientProvider, PulsarConsumerConfig, PulsarConsumerProvider, PulsarProducerConfig, PulsarProducerProvider}
import zio.{Task, _}
import zio.console.{putStr, putStrLn, Console}

import java.util.UUID
import zio.json._

object Back extends App {

  val host: String = sys.env.getOrElse("PULSAR_HOST", "localhost")

  val client: ZLayer[Any, Throwable, Has[PulsarClient]] = ZLayer.succeed(
    PulsarClientConfig(serviceUrl = s"pulsar://$host:6650")
  ) >>> PulsarClientProvider.layer

  val producer: ZLayer[Any, Throwable, Has[Producer[String]]] =
    (client ++ ZLayer.succeed(
      PulsarProducerConfig(topic = "caliban")
    )) >>> PulsarProducerProvider.layer

  val consumer: ZLayer[Any, Throwable, Has[Consumer[String]]] =
    (client ++ ZLayer.succeed(
      PulsarConsumerConfig(
        topics = List("back"),
        subscription = s"back-shared",
        subscriptionType = SubscriptionType.Shared
      )
    )) >>> PulsarConsumerProvider.layer

  val deps =
    ZEnv.live ++ UserEventRepository.live ++ producer ++ consumer ++ RedisProvider.redisPool

  def consumerLogic(producer: Producer[String], consumer: Consumer[String]): ZIO[Console with Has[JedisPool], Throwable, Unit] = for {
    msg      <- Task(consumer.receive())
    _        <- Task(consumer.acknowledge(msg)) // auto ack
    str      <- Task(new String(msg.getData))
    _        <- putStrLn(s"Received: $msg")
    model    <- ZIO.fromEither(str.fromJson[UserEvent]).mapError(err => new Exception(err))
    response <- RedisProvider.managedRedis.use { redis =>
      Task(redis.set(model.id, model.toJson))
        .map(status => ResponseEvent(id = model.id, status = status))
    }
    mId      <- Task(producer.send(response.toJson))
    _        <- putStrLn(s"Sent ${response.toJson} with MessageId ${mId.toString}")
  } yield ()

  val program = for {
    _        <- putStrLn("Starting Back...")
    producer <- ZIO.service[Producer[String]]
    consumer <- ZIO.service[Consumer[String]]
    _        <- consumerLogic(producer, consumer).forever
  } yield ExitCode.success

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    program.provideLayer(deps).exitCode
}
