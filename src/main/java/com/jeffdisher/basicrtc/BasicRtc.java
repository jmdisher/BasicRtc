package com.jeffdisher.basicrtc;

import java.io.File;
import java.net.InetSocketAddress;

import org.eclipse.jetty.util.resource.PathResource;

import com.jeffdisher.breakwater.RestServer;


public class BasicRtc 
{
	public static void main( String[] args )
	{
		if (args.length == 2)
		{
			int port = Integer.parseInt(args[0]);
			File directory = new File(args[1]);
			if (directory.isDirectory())
			{
				// We will listen on every interface.
				InetSocketAddress interfaceToBind = new InetSocketAddress(port);
				PathResource staticResource = new PathResource(directory);
				// We want to disable static resource caching (at least for now).
				RestServer server = new RestServer(interfaceToBind, staticResource, "no-store,no-cache,must-revalidate");
				
				// We only have the one end-point.
				server.addWebSocketFactory("/chat/{string}", "setup", new WS_ChatSetup());
				
				// We don't bother with an explicit "stop", just relying on ctrl-c.
				System.out.println("Listening on all local interfaces via port: " + port);
				System.out.println("Hosting static content from directory: " + directory.getAbsolutePath());
				System.out.println("Press Ctrl-C to stop server...");
				server.start();
			}
			else
			{
				System.err.println("Resource path is not a directory: " + directory.getAbsolutePath());
				System.exit(2);
			}
		}
		else
		{
			System.err.println("BasicRtc PORT RESOURCE_PATH");
			System.exit(1);
		}
	}
}
