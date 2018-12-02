package jp.kenichi.pk1
package client

import scala.scalajs.js

import org.log4s.getLogger

object ClientMain {
	// FIXME log4s-1.6.1: timePart is in UTC.  Also the fraction separator should be '.' rather than ',' (like the jvm counterpart)
//	import org.log4s.Log4sConfig
//	import org.log4s.log4sjs.LoggedEvent
//	Log4sConfig.setLoggerAppenders("", false, Seq({ ev: LoggedEvent => () }))

	val log = getLogger

	def main(args: Array[String]) {
		log.info("Hello 世界")
	}
}
