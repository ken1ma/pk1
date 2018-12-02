package jp.kenichi.pk1
package server

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import cats.effect.IO
import org.log4s.getLogger

/** Provides the API services (REST JSON) */
object ApiService {
	val log = getLogger

	val mainService = HttpService[IO] {
		case GET -> Root / "ping" => Ok("pong")
	}
}
