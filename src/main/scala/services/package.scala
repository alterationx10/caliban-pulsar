import org.apache.pulsar.client.api.{AuthenticationFactory, ClientBuilder, Consumer, Producer, PulsarClient, Schema, SubscriptionMode, SubscriptionType}
import zio._

import scala.jdk.CollectionConverters._

package object services {
  case class PulsarClientConfig(serviceUrl: String, jwt: Option[String] = None)
  case class PulsarProducerConfig(topic: String)
  case class PulsarConsumerConfig(topics: List[String], subscription: String)

  implicit class ExtendedPulsarClientBuilder(builder: ClientBuilder) {

    def buildFromConfig(config: PulsarClientConfig): PulsarClient = {

      builder.serviceUrl(config.serviceUrl)

      config.jwt.foreach { token =>
        builder.authentication(AuthenticationFactory.token(token))
      }

      builder.build()
    }

  }

  implicit class ExtendedPulsarClient(client: PulsarClient) {
    def newConsumerFromConfig(config: PulsarConsumerConfig): Consumer[String] = {
      client
        .newConsumer(Schema.STRING)
        .topics(config.topics.asJava)
        .subscriptionName(config.subscription)
        .subscriptionType(SubscriptionType.Shared) // Caliban should be exclusive, but we start with a unique ID so no competition
        .subscribe()
    }
    def release: UIO[Unit] = Task.effectTotal(client.close())
  }

  implicit class ExtendedPulsarProducer[T](producer: Producer[T]) {
    def release: UIO[Unit] = Task.effectTotal(producer.close())
  }

  implicit class ExtendedPulsarConsumer[T](consumer: Consumer[T]) {
    def release: UIO[Unit] = Task.effectTotal(consumer.close())
  }
}
