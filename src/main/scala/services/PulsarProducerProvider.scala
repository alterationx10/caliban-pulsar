package services

import zio._
import org.apache.pulsar.client.api.{Producer, PulsarClient, Schema}

object PulsarProducerProvider {

  private val acquire: ZIO[Has[PulsarClient] with Has[
    PulsarProducerConfig
  ], Throwable, Producer[String]] =
    for {
      config <- ZIO.service[PulsarProducerConfig]
      client <- ZIO.service[PulsarClient]
    } yield client.newProducer(Schema.STRING).topic(config.topic).create()

  val managed: ZManaged[Has[PulsarClient] with Has[
    PulsarProducerConfig
  ], Throwable, Producer[String]] =
    ZManaged.make(acquire)(_.release)

  val layer: ZLayer[Has[PulsarClient] with Has[
    PulsarProducerConfig
  ], Throwable, Has[Producer[String]]] =
    ZLayer.fromManaged(managed)

}
