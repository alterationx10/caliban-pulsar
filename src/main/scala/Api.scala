
import caliban.GraphQL.graphQL
import caliban.{GraphQL, RootResolver}
import caliban.schema.{GenericSchema, Schema}
import domain.UserEvent
import org.apache.pulsar.client.api.Producer
import repository.UserEventRepository
import schema.{EventArgs, Mutations, Queries}
import zio._
import zio.json._

import scala.language.postfixOps

object Api extends GenericSchema[
  Has[UserEventRepository] with Has[Producer[String]]
]{


  implicit val argSchema: Schema[Any, EventArgs] = Schema.genMacro[EventArgs].schema
  implicit val userEventSchema: Schema[Any, UserEvent] = Schema.genMacro[UserEvent].schema


  val api = graphQL(
    RootResolver(
      Queries(args => UserEventRepository(_.get(args.id))),
      Mutations(arg => ZIO.serviceWith[Producer[String]](p => Task(p.send(arg.toJson).toString)))
    )
  )


}

