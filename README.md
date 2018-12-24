# Server Environment

1. Java 11.0.1
	1. Either OpenJDK or Oracle JDK


# Client Environment

1. Latest Chrome/Firefox/Safari/Edge


# Development Environment

In addition to the server environment above
1. [mill](http://www.lihaoyi.com/mill/) 0.3.5
	1. On macOS X where both Java 11 and 8 are installed, JAVA_HOME must be set for Java 11, PATH must contain $JAVA_HOME/bin, and existing mill processes must be killed if they have been spawned with Java 8


# Frequently Used Development Commands

1. Run the server in the background (that kills the previous server process)

		mill server.runBackground

	1. `tail -F log/server-debug.log` to see the debug log

2. Generate JavaScript fast

		mill client.fastOpt

	1. Open `https://localhost:8443/` to test

3. Build the jar containining both the server and the client

		mill dist.jar


# Infrequently Used Development Commands

1. Start REPL

		mill -i server.repl

2. Show the dependencies

		mill _.ivyDepsTree
