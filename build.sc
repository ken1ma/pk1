import mill._, scalalib._, scalajslib._

trait CommonScalaModule extends ScalaModule {
	def scalaVersion = "2.12.7" // 2.12.8 has been released but mill repl fails to resolve com.lihaoyi:ammonite_2.12.8:1.4.4
	def scalacOptions = Seq(
		"-Ypartial-unification", // for http4s
		"-deprecation",
		"-feature",
	)
}

object server extends CommonScalaModule {
	def ivyDeps = Agg(
		ivy"org.http4s::http4s-dsl:0.18.21", // check https://http4s.org/versions/
		ivy"org.http4s::http4s-blaze-server:0.18.21",
		ivy"org.http4s::http4s-circe:0.18.21",
		ivy"io.circe::circe-config:0.5.0", // json configuration
		ivy"org.log4s::log4s:1.6.1", // slf4j wrapper
		ivy"org.apache.logging.log4j:log4j-slf4j-impl:2.11.1", // log implementation
	)
	def moduleDeps = Seq(shared)

	// serve the output of client.fastOpt
	def forkEnv = Map(
		"DOC_MAPPINGS" -> "/pk1.js -> /out.js",
		"DOC_ROOTS" -> "out/client/fastOpt/dest, client/resources",
	)
}

object client extends ScalaJSModule with CommonScalaModule {
	def scalaJSVersion = "0.6.26"
	def ivyDeps = Agg(
		ivy"org.log4s::log4s_sjs0.6:1.6.1", // TODO: can "_sjs0.6" be removed?
//		ivy"org.scala-js::scalajs-dom:0.9.6",
	)
	def moduleDeps = Seq(shared)
}

object shared extends CommonScalaModule {
	def ivyDeps = Agg(
		ivy"io.circe::circe-core:0.10.0", // json decoding/encoding
		ivy"io.circe::circe-generic:0.10.0",
		ivy"io.circe::circe-parser:0.10.0",
	)
}
