package jp.kenichi.pk1.server

import com.typesafe.config.{ConfigFactory, ConfigException}
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.config.syntax._
import cats.syntax.either._
import org.log4s.getLogger

/** The server configuration */
object ServerConf {
	val log = getLogger

	def errorFatal(message: String, ex: Throwable) = { // the return type is Nothing
		log.error(s"$message: $ex") // don't output the stack trace on the error level
		log.debug(ex)(s"$message")
		sys.exit(254) // 255 for command line argument error
	}

	val conf = try { ConfigFactory.load } catch {
		case ex: ConfigException => errorFatal("failed to parse the conf", ex)
	}

	protected def getTopLevel[A](name: String)(implicit decoder: Decoder[A]) = conf.as[A](name).valueOr(errorFatal(s"failed to decode conf: $name", _))

	val https = getTopLevel[Https]("https")
	case class Https(host: String, port: Int, ssl: Ssl)
	case class Ssl(keyStorePath: String, keyStorePassword: String, keyManagerPassword: String)

	val http = getTopLevel[Http]("http")
	case class Http(host: String, port: Int)
}
