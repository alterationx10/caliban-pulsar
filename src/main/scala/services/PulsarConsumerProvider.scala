package services
import org.apache.pulsar.client.api.{Consumer, PulsarClient}
import zio._

object PulsarConsumerProvider {

  private val acquire = for {
    config <- ZIO.service[PulsarConsumerConfig]
    client <- ZIO.service[PulsarClient]
  } yield client.newConsumerFromConfig(config)

  val managed: ZManaged[Has[PulsarClient] with Has[
    PulsarConsumerConfig
  ], Nothing, Consumer[String]] =
    ZManaged.make(acquire)(_.release)

  val layer: ZLayer[Has[PulsarClient] with Has[
    PulsarConsumerConfig
  ], Nothing, Has[Consumer[String]]] = ZLayer.fromManaged(managed)
}
