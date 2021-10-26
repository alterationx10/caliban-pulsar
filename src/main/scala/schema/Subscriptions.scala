package schema

import domain.{EventArgs, ResponseEvent}
import services.SubscriptionService
import zio._
import zio.stream._

case class Subscriptions(
    events: EventArgs => ZStream[Has[
      SubscriptionService
    ], Throwable, ResponseEvent]
)
