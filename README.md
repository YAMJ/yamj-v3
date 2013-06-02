YAMJ-v3
=======

Main Project for YAMJ v3

Modules
-------
### yamj3-common:
> Holds the commonly used classes.

### yamj3-core:
> Main webapp for YAMJ3 core; entry point for everything.
This is a real webapp; can be started with a jetty server or deployed to an existing tomcat server.

### yamj3-jetty:
> Starts the yamj3-core if you do not have, or want to use a tomcat server.

### yamj3-filescanner:
> The file scanner for processing the media directories and sending the file information to the core processor.

First Try
---------
You just can run: JettyCore from the yamj3-jetty package. Then a jetty server on port 8888 will be started.
