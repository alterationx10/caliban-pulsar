package services
import org.apache.pulsar.client.api.PulsarClient
import zio._

object PulsarClientProvider {

  private val acquire: RIO[Has[PulsarClientConfig], PulsarClient] =
    for {
      config <- ZIO.service[PulsarClientConfig]
    } yield PulsarClient.builder().buildFromConfig(config)

  val managed: ZManaged[Has[PulsarClientConfig], Throwable, PulsarClient]  = ZManaged.make(acquire)(_.release)
  val layer: ZLayer[Has[PulsarClientConfig], Throwable, Has[PulsarClient]] = ZLayer.fromManaged(managed)
}
