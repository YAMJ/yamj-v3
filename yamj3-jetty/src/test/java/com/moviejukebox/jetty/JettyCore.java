package com.moviejukebox.jetty;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyCore {
    
    public static void main(final String[] aArgs) throws Exception {
        BasicConfigurator.configure();
        final Server server = new Server(8888);
        server.setHandler(createWebAppContext());
        
        server.start();
        server.join();
    }

    private static WebAppContext createWebAppContext() {
        final WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setBaseResource(new ResourceCollection(new String[] { "./../yamj3-core/src/main/webapp/" }));
        return context;
    }
}