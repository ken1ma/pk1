# Server Environment

1. Java 11.0.1
	1. Either OpenJDK or Oracle JRE


# Client Environment

1. Chrome for now


# Development Environment

1. Java 1.8.0_191
1. [mill](http://www.lihaoyi.com/mill/) 0.3.5


# Frequently Used Development Commands

1. Run the server in the background (that kills the previous server process)

		mill server.runBackground

	1. `tail -F log/server-debug.log` to see the debug log

1. Generate JavaScript fast

		mill client.fastOpt

	1. Open `https://localhost:8443/` to test


# Infrequently Used Development Commands

1. Build the assembly jar `out/server/assembly/dest/out.jar` that contains both the server and the client

		mill server.assembly

2. Show the dependencies

		mill _.ivyDepsTree
