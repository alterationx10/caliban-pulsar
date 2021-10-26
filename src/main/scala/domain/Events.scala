package domain

import zio.json.{DeriveJsonCodec, JsonCodec}

case class UserEvent(
                      id: String,
                      message: String
                    )

object UserEvent {
  implicit val codec: JsonCodec[UserEvent] = DeriveJsonCodec.gen[UserEvent]
}

case class ResponseEvent(
                          id: String,
                          status: String
                        )

object ResponseEvent {
  implicit val codec = DeriveJsonCodec.gen[ResponseEvent]
}