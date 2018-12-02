package jp.kenichi.pk1.server

import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.ShutdownHookThread
import java.lang.management.ManagementFactory
import java.net.URLDecoder

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.headers.{Host, Location}
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.http4s.server.Router
import org.http4s.server.SSLKeyStoreSupport.StoreInfo
import org.http4s.server.staticcontent.{resourceService, ResourceService}
import org.http4s.server.staticcontent.{fileService, FileService}
import org.http4s.server.blaze.BlazeBuilder
import cats.effect.IO
import cats.data.{Kleisli, OptionT}
import fs2.{StreamApp, Scheduler}
import org.log4s.{getLogger, Logger, MDC}

object ServerMain extends StreamApp[IO] with ServerHelper {
	val log = getLogger
	log.info(s"startup ($pid)")
	ShutdownHookThread(log.info(s"shutdown ($pid)")) // find out when the process dies unexpectedly
	logRuntimeInfo
	logRuntimeState

	// the root service
	val rootService = HttpService[IO] {
		// unauthenticated health check
		case GET -> Root / "ping" => Ok("pong")

		// serve index.html for whatever path set by history.pushState
		// except those that contain '.' in the last path component (e.g. favicon.ico, robots.txt)
		case req @ GET -> _ if (!req.uri.path.split('/').lastOption.exists(_.contains('.'))) =>
			StaticFile.fromResource("/web/index.html", Some(req), preferGzipped = true)
					.getOrElseF(NotFound())
	}

	// the server
	lazy val conf = ServerConf
	def sslStream(/*implicit scheduler: Scheduler*/) = {
		var services = Seq[(String, HttpService[IO])]() // the last element has the highest precedence
		services :+= "/" -> rootService
		services :+= "/" -> UrlDecodePathInfo(resourceService(ResourceService.Config("/web", "/", preferGzipped = true))) // js/css
		for (docRoot <- docRoots.reverse)
			services :+= "/" -> UrlDecodePathInfo(fileService(FileService.Config(docRoot, "/")), docMappings)
		services :+= "/api" -> ApiService.mainService

		log.info(s"accepting HTTPS connection on ${conf.https.host}:${conf.https.port}")
		BlazeBuilder[IO].bindHttp(conf.https.port, conf.https.host)
				.withSSL(StoreInfo(conf.https.ssl.keyStorePath, conf.https.ssl.keyStorePassword), conf.https.ssl.keyManagerPassword)
				.mountService(LogMiddle(Router(services: _*)))
				.serve

		// TODO: should org.http4s.server.middleware.HSTS (HTTP Strict Transport Security) be used?
	}

	// redirect HTTP to HTTPS
	def redirectStream = {
		log.info(s"accepting HTTP connection on ${conf.http.host}:${conf.http.port}")
		BlazeBuilder[IO].bindHttp(conf.http.port, conf.http.host)
				.mountService(redirectService, "")
				.serve
	}

	// taken from https://github.com/http4s/http4s/blob/master/examples/src/main/scala/com/example/http4s/ssl/SslExampleWithRedirect.scala#L29
	val redirectService = HttpService[IO] {
		case req => req.headers.get(Host) match {
			case Some(Host(host, _)) =>
				val baseUri = req.uri.copy(
					// force https
					scheme = Some(Scheme.https),
					// change the port
					authority = Some(
						Authority(
							req.uri.authority.flatMap(_.userInfo),
							RegName(host),
							port = ServerConf.https.port match {
								case 443 => None
								case _ => Some(ServerConf.https.port)
							}
						)
					)
				)
				MovedPermanently(Location(baseUri.withPath(req.uri.path)))

			case _ => BadRequest("No Host header")
		}
	}

	// taken from https://github.com/http4s/http4s/blob/master/examples/src/main/scala/com/example/http4s/ssl/SslExampleWithRedirect.scala#L61
	override def stream(args: List[String], requestShutdown: IO[Unit]) = {
		Scheduler[IO](corePoolSize = 2).flatMap { implicit scheduler =>
			sslStream.mergeHaltBoth(redirectStream) // halts as soon as either branch halts
		}
	}
}

/** A very crude logging middleware */
object LogMiddle {
	val log = getLogger

	def withLog(req: Request[IO]) = {
		log.debug(s"${req.method} ${URLDecoder.decode(req.uri.toString, "UTF-8")}") // ${req.headers} could be added
		req
	}

	def apply(service: HttpService[IO]): HttpService[IO] = Kleisli { req =>
		MDC.withCtx ("remote" -> req.remoteAddr.getOrElse("")) {
			OptionT(service(withLog(req)).value)
		}
	}
}

// Allow Japanese file names
// http4s-0.18.5
// URL encoded pathInfo always result in 404
// https://github.com/http4s/http4s/blob/master/server/src/main/scala/org/http4s/server/staticcontent/FileService.scala#L48
object UrlDecodePathInfo {
	def apply(service: HttpService[IO], mappings: Map[String, String] = Map.empty): HttpService[IO] = Kleisli { req =>
		val pathInfo = URLDecoder.decode(req.pathInfo, "UTF-8")
		service(req.withPathInfo(mappings.get(pathInfo).getOrElse(pathInfo)))
	}
}

trait ServerHelper {
	def log: Logger

	val pid = ManagementFactory.getRuntimeMXBean.getPid

	import scala.util.Properties

	def logRuntimeInfo {
		//log.debug(s"appVersion ${BuildInfo.version}, gitHead ${BuildInfo.gitHead.take(7)} (${BuildInfo.gitStatus}), builtAt ${BuildInfo.builtAtLocalDateTimeString}")

		import Properties.{javaVendor, javaVersion, javaVmVendor, javaVmName, javaVmVersion, javaVmInfo, javaHome, osName}
		log.debug(s"javaVendor = $javaVendor, javaVersion = $javaVersion")
		log.debug(s"javaVmVendor = $javaVmVendor, javaVmName = $javaVmName, javaVmVersion = $javaVmVersion, javaVmInfo = $javaVmInfo")
		log.debug(s"javaHome = $javaHome") // there might be multiple installation with different policies
		log.debug(s"osName = $osName, osArch = ${System.getProperty("os.arch")}, osVersion = ${System.getProperty("os.version")}")
		//import scala.util.Properties.{javaSpecVendor, javaSpecName, javaSpecVersion}
		//log.debug(s"javaSpecVendor = $javaSpecVendor, javaSpecName = $javaSpecName, javaSpecVersion = $javaSpecVersion")
	}

	def logRuntimeState {
		val runtime = Runtime.getRuntime
		log.debug(s"totalMemory ${runtime.totalMemory / 1024 / 1024}MB / maxMemory ${runtime.maxMemory / 1024 / 1024}MB")
	}

	/** DOC_ROOTS environment variable */
	lazy val docRoots = try {
		val docRoots = Properties.envOrNone("DOC_ROOTS") match {
			case Some(text) => text.split(',').toList.map(_.trim) // toList so that toString shows the elements
			case None => Nil
		}
		log.debug(s"docRoots = $docRoots")
		docRoots

	} catch {
		case ex: Throwable => ServerConf.errorFatal("Unexpected environment variable DOC_ROOTS", ex)
	}

	/** DOC_MAPPINGS environment variable */
	lazy val docMappings = try {
		val docMappings = Properties.envOrNone("DOC_MAPPINGS") match {
			case Some(text) => text.split(',').map(_.trim).map { entry =>
				val sep = entry.indexOf("->")
				if (sep == -1)
					throw new Exception("An element of environment variable DOC_MAPPINGS does not contain \"->\": $text")
				(entry.take(sep).trim, entry.drop(sep + 2).trim)
			}.toMap
			case None => Map.empty[String, String]
		}
		log.debug(s"docMappings = $docMappings")
		docMappings

	} catch {
		case ex: Throwable => ServerConf.errorFatal("Unexpected environment variable DOC_MAPPINGS", ex)
	}
}
