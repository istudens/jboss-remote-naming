/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.naming.remote;

import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.naming.remote.client.InitialContextFactory;
import org.jboss.naming.remote.client.RemoteContext;
import org.jboss.naming.remote.client.ejb.RemoteNamingEjbClientContextSelector;
import org.jboss.naming.remote.protocol.IoFutureHelper;
import org.jboss.naming.remote.server.RemoteNamingService;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.jboss.remoting3.spi.NetworkServerProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.xnio.Options.SSL_ENABLED;

/**
 * @author John Bailey
 */
public class FinalizerTest {
    private static RemoteNamingService server;

    private static final Context localContext = new MockContext();

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Xnio xnio = Xnio.getInstance();
        final Endpoint endpoint = Remoting.createEndpoint("RemoteNaming", xnio, OptionMap.EMPTY);
        endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.EMPTY);

        final NetworkServerProvider nsp = endpoint.getConnectionProviderInterface("remote", NetworkServerProvider.class);
        final SocketAddress bindAddress = new InetSocketAddress("localhost", 7999);
        final OptionMap serverOptions = TestUtils.createOptionMap();

        nsp.createServer(bindAddress, serverOptions, new TestUtils.DefaultAuthenticationHandler(), null);
        server = new RemoteNamingService(localContext, Executors.newFixedThreadPool(10));
        server.start(endpoint);
    }

    private Context getRemoteContext() throws Exception {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, InitialContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, "remote://localhost:7999");
        env.put("jboss.naming.client.ejb.context", "false");
        return new InitialContext(env);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stop();
    }

    @Test
    public void testLookup() throws Exception {
        localContext.bind("test", "TestValue");
        assertEquals("TestValue", getRemoteContext().lookup("test"));
        localContext.unbind("test");
    }

}
