package repository

import domain.UserEvent
import domain.UserEvent._
import repository.RedisProvider.managedRedis
import zio._
import zio.json._

trait UserEventRepository {
  def get(id: String): Task[Option[UserEvent]]

  def persist(event: UserEvent): Task[String]
}

case class UserEventRepositoryLive() extends UserEventRepository {

  override def get(id: String): Task[Option[UserEvent]] = for {
    data <- managedRedis.use { client =>
      Task(client.get(id))
    }.provideLayer(RedisProvider.redisPool)
    json <- Task(Option(data).flatMap(_.fromJson[UserEvent].toOption))
  } yield json

  override def persist(event: UserEvent): Task[String] = for {
    status <- managedRedis.use { client =>
      Task(client.set(event.id, event.toJson))
    }.provideLayer(RedisProvider.redisPool)
  } yield status
}

object UserEventRepository extends Accessible[UserEventRepository] {
  val live: ULayer[Has[UserEventRepository]] = ZLayer.succeed(UserEventRepositoryLive())
}
