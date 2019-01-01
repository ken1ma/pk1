package jp.kenichi.pk1.shared

import scala.io.Source
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.{UTF_8, ISO_8859_1}

case class Pem(bin: Array[Byte], label: String)

/** Privacy-Enhanced Mail format */
object Pem {
	val headerPrefix = "-----BEGIN "
	val headerSuffix = "-----"

	val footerPrefix = "-----END "
	val footerSuffix = "-----"

	// fail early for the illegal content
	val illegalContentPrefix = "-----"

	/** Can be used to determine if a binary is PEM or DER */
	val headerPrefixBin = headerPrefix.getBytes(ISO_8859_1)

	case class DecodeException(message: String) extends Exception(message)

	def decode(lines: Iterator[String]) = {
		val sb = new StringBuilder
		var label: Option[String] = None

		// ignore the leading spaces, trailing spaces, and empty lines
		val pems = lines.map(_.trim).filter(_.nonEmpty).flatMap { line => label match {
			// header
			case None =>
				if (line.startsWith(headerPrefix) && line.endsWith(headerSuffix))
					label = Some(line.drop(headerPrefix.length).dropRight(headerSuffix.length).trim)

				else
					throw new DecodeException(s"""expected "${headerPrefix}label$headerSuffix": $line""")

				None

			case Some(headerLabel) =>
				// footer
				if (line.startsWith(footerPrefix) && line.endsWith(footerSuffix)) {
					val footerLabel = line.drop(footerPrefix.length).dropRight(footerSuffix.length).trim
					if (footerLabel != headerLabel)
						throw new DecodeException(s"expected $headerLabel but found $footerLabel: $line")

					val bin = Base64.decode(sb.toString)
					sb.clear
					label = None

					Some(Pem(bin, headerLabel))

				// base64
				} else {
					if (line.startsWith(illegalContentPrefix))
						throw new DecodeException(s"expected base64 or footer: $line")

					else
						sb ++= line

					None
				}
		} }

		label match {
			case None =>
			case Some(label) => throw new DecodeException(s"""expected "${footerPrefix}$label$footerSuffix"""")
		}

		pems.toSeq
	}
	def decode(lines: String): Seq[Pem] = decode(Source.fromString(lines).getLines)
	def decode(lines: Array[Byte], charset: Charset = UTF_8): Seq[Pem] = decode(new String(lines, charset))
}
