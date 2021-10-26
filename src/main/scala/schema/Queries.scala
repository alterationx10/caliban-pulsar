package schema

import zio._
import domain.UserEvent
import repository.UserEventRepository

case class EventArgs(id: String)

case class Queries(
    event: EventArgs => RIO[Has[UserEventRepository], Option[UserEvent]]
)
