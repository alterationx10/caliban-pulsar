package schema

import zio._
import domain.{EventArgs, UserEvent}
import repository.UserEventRepository

case class Queries(
    event: EventArgs => RIO[Has[UserEventRepository], Option[UserEvent]]
)
