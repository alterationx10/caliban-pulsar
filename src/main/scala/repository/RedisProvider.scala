package repository

import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}
import zio._

object RedisProvider {
  val host = "host.docker.internal"

  val redisPool: ZLayer[Any, Throwable, Has[JedisPool]] = ZLayer.fromAcquireRelease {
    Task {
      import java.time.Duration
      val poolConfig = new JedisPoolConfig
      poolConfig.setMaxTotal(10)
      poolConfig.setMaxIdle(10)
      poolConfig.setMinIdle(5)
      poolConfig.setTestOnBorrow(true)
      poolConfig.setTestOnReturn(true)
      poolConfig.setTestWhileIdle(true)
      poolConfig.setSoftMinEvictableIdleTime(Duration.ofSeconds(60))
      poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30))
      poolConfig.setNumTestsPerEvictionRun(3)
      poolConfig.setBlockWhenExhausted(true)
      new JedisPool(poolConfig, host)
    }
  }(r => ZIO.succeed(r.close()))

  val managedRedis: ZManaged[Has[JedisPool], Throwable, Jedis] =
    ZManaged.make(
      ZIO.serviceWith[JedisPool](pool => Task(pool.getResource))
    )(r => ZIO.succeed(r.close()))
}
