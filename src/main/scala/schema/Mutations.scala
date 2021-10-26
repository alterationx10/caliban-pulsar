package schema

import domain.UserEvent
import org.apache.pulsar.client.api.Producer
import zio._

case class Mutations(
  save: UserEvent => RIO[Has[Producer[String]], String]
)
