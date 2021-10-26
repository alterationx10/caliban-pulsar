import caliban.GraphQL.graphQL
import caliban.RootResolver
import caliban.schema.{GenericSchema, Schema}
import domain.{EventArgs, UserEvent}
import org.apache.pulsar.client.api.{Consumer, Producer}
import repository.UserEventRepository
import schema.{Mutations, Queries, Subscriptions}
import services.SubscriptionService
import zio._
import zio.json._

import scala.language.postfixOps

object Api
    extends GenericSchema[
      Has[UserEventRepository] with Has[Producer[String]] with Has[SubscriptionService] with Has[Consumer[String]]
    ] {

  implicit val argSchema: Schema[Any, EventArgs]       = Schema.genMacro[EventArgs].schema
  implicit val userEventSchema: Schema[Any, UserEvent] = Schema.genMacro[UserEvent].schema

  val api = graphQL(
    RootResolver(
      Queries(args => UserEventRepository(_.get(args.id))),
      Mutations(arg => ZIO.serviceWith[Producer[String]](p => Task(p.send(arg.toJson).toString))),
      Subscriptions(arg => SubscriptionService.events(arg.id))
    )
  )

}
