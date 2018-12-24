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
		"DOC_ROOTS" -> "out/client/fastOpt/dest, client/resources/web",
	)
}

object dist extends Module {
	def jar = T {
		val jsName = "pk1.js"
		val outFile = os.pwd / "dist" / "pk1.jar"
		os.makeDir.all(outFile / os.up)

		import scala.collection.JavaConverters._
		import java.io.FileOutputStream
		import java.util.jar.{JarOutputStream, JarEntry, JarFile}
		val out = new JarOutputStream(new FileOutputStream(outFile.toIO))

		try {
			// server jar
			val serverJar = new JarFile(server.assembly().path.toIO)
			try {
				val buf = new Array[Byte](8192)
				for (entry <- serverJar.entries.asScala) {
					out.putNextEntry(entry)
					val in = serverJar.getInputStream(entry)
					try {
						Iterator.continually(in.read(buf)).takeWhile(_ != -1).foreach(len => out.write(buf, 0, len))
					} finally {
						in.close
					}
				}
			} finally {
				serverJar.close
			}

			// client js
			def add(name: String, file: os.Path) {
				if (os.isDir(file)) {
					for (child <- os.list.stream(file))
						if (!child.last.startsWith(".")) {
							add(s"$name/${child.last}", child)
						}
				} else {
					val entry = new JarEntry(name)
					entry.setTime(os.mtime(file))
					out.putNextEntry(entry)
					out.write(os.read.bytes(file))
				}
			}
			add(s"web/$jsName", client.fullOpt().path)

			// client html
			for (resource <- client.resources())
				for (child <- os.list.stream(resource.path))
					if (child.last == "web")
						add("web", child)

		} finally {
			out.close
		}

		outFile
	}
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
