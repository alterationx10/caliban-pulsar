package domain

import zio.json.{DeriveJsonCodec, JsonCodec}

case class ResponseEvent(
    id: String,
    status: String
)

object ResponseEvent {
  implicit val codec: JsonCodec[ResponseEvent] = DeriveJsonCodec.gen[ResponseEvent]
}
