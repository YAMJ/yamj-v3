YAMJ-v3
=======

Main Project for YAMJ v3

Modules
-------
yamj3-core: Main webapp for YAMJ3 core; entry point for everything.
This is a real webapp; can be started with a jetty server or deployed to an existing tomcat.

yamj3-common: Holds the commonly used classes, until now just one interface for a ping service.

yamj3-batch: The batch processing, parse in command line arguments and executes a batch on the YAMJ3 core server
With HTTP invoker there can be parsed serializable objects to the YAMJ3 core server.

yamj3-jetty: Until now just a first attempt to start a jetty server.

yamj3-distribution: The distribution files as known from yamj2.

First Try
---------
You just can run: JettyCore from the yamj3-jetty package. Then a jetty server onb port 8889 will be started.

Also you can run MoviejukeboxBatch with parameter "-b ping".
Then yamj3-batch calls the exposed service "PingService" on the running jetty server and returns a result.
