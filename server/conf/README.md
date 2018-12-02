1. The self-signed certificate `localhost.p12` has been generated with JDK 11.0.1

        keytool -genkeypair \
            -keyalg RSA -keysize 2048 \
            -dname "CN=localhost" -validity 3650 \
            -ext SubjectAlternativeName=dns:localhost \
            -keystore localhost.p12 -storetype pkcs12 -storepass changeit \
            -alias localhost

	1. p12 file can be dumped with

            openssl pkcs12 -info -in localhost.p12

	1. Chrome 70 says "not secure (broken HTTPS)" "Subject Alternative Name missing" in the developer security tab if `SubjectAlternativeName` is not specified

	1. If `-storetype` is not specified, the file would be in JKS format
		1. jdk-1.8.0_191: `keytool` emits the warning "The JKS keystore uses a proprietary format. It is recommended to migrate to PKCS12 which is an industry standard format"
		2. jdk-11.0.1: `keytool` does not emit the warning
