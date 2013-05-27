package com.yamj.jetty;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

public class Start {

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();

        String yamjHome = System.getProperty("yamj3.home", ".");

        Server server = new Server(8888);
        server.setGracefulShutdown(3000);
        server.setStopAtShutdown(true);

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(yamjHome + "/lib/yamj3-core-3.0-SNAPSHOT.war");
        server.setHandler(webapp);

        if (server.getThreadPool() instanceof QueuedThreadPool) {
            ((QueuedThreadPool) server.getThreadPool()).setMaxIdleTimeMs(2000);
        }

        server.start();
        server.join();
    }
}
