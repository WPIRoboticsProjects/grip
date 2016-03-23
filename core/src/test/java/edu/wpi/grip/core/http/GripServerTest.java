
package edu.wpi.grip.core.http;

import com.google.inject.Inject;
import com.sun.net.httpserver.HttpServer;
import edu.wpi.grip.core.MockPipeline;
import edu.wpi.grip.core.MockPipeline.MockProjectSettings;

import edu.wpi.grip.core.http.GripServer.HttpServerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class GripServerTest {

    private static final int GRIP_SERVER_TEST_PORT = 2084;
    private final DefaultHttpClient client;

    private HttpServer server;
    private int port;

    public static class TestServerFactory implements HttpServerFactory {

        @Override
        public HttpServer create(int port) {
            HttpServer server;
            for(int offset = 0;; offset++) {
                try {
                    server = HttpServer.create(new InetSocketAddress("localhost", port + offset), 1);
                    break;
                } catch (IOException e) {
                    continue;
                }
            }
            return server;
        }

    }

    // DON'T USE INJECTION FOR THIS
    private GripServer instance;

    public GripServerTest() {
        for (int portOffset = 0;; portOffset++) {
            try {
                server = HttpServer.create(new InetSocketAddress("localhost", GRIP_SERVER_TEST_PORT + portOffset), 1);
                port = GRIP_SERVER_TEST_PORT + portOffset;
                break;
            } catch (IOException ex) {
                // That port is taken -- keep trying different ports
            }
        }
        MockProjectSettings mockSettings = new MockProjectSettings();
        mockSettings.setServerPort(port);
        instance = new GripServer((ignore) -> server, new MockPipeline(mockSettings));
        instance.start();
        
        client = new DefaultHttpClient();
        client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
    }

    /**
     * Test of GetHandler methods, of class GripServer.
     */
    @Test
    public void testGetHandlers() {
        System.out.println("testGetHandlers");
        String path = "/testGetHandlers";
        GetHandler handler = params -> path;
        instance.addGetHandler(path, handler);
        instance.addDataSupplier(path, () -> path);
        try {
            String data = doGet(path);
            assertEquals(data, path);
        } catch (IOException ex) {
            fail(ex.getMessage());
        } finally {
            instance.removeGetHandler(path); // cleanup
        }
    }

    /**
     * Test of addPostHandler method, of class GripServer.
     */
    @Test
    public void testAddPostHandler() {
        System.out.println("addPostHandler");
        String path = "/testAddPostHandler";
        byte[] testBytes = "testAddPostHandler".getBytes();
        PostHandler handler = bytes -> Arrays.equals(bytes, testBytes);
        instance.addPostHandler(path, handler);
        try {
            doPost(path, testBytes);
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            instance.removePostHandler(handler); // cleanup
        }
    }

    /**
     * Test of data supplier methods
     */
    @Test
    public void testDataSuppliers() {
        System.out.println("testDataSuppliers");
        String name = "testDataSuppliers";
        Supplier<?> supplier = () -> name;
        instance.addDataSupplier(name, supplier);
        assertTrue(instance.hasDataSupplier(name));
        instance.removeDataSupplier(name);
        assertFalse(instance.hasDataSupplier(name));
    }

    /**
     * Test of start and stop methods, of class GripServer.
     */
    @Test
    public void testStartStop() {
        System.out.println("start");
        try {
            System.out.println("Starting server");
            instance.start(); // should do nothing since the server's already running
            System.out.println("Stopping server (1/2)");
            instance.stop();  // stop the server so we know we can start it
            System.out.println("Stopping server (2/2)");
            instance.stop();  // second call should do nothing
            System.out.println("Restarting server (1/2)");
            instance.start(); // restart the server
            System.out.println("Restarting server (2/2)");
            instance.start(); // second call should do nothing
            instance.restart();
        } catch (Exception e) {
            System.out.println("FAILED");
            fail(e.getMessage());
        } finally {
            System.out.println("Ensuring server is in a 'started' state");
            instance.start(); // make sure the server is running so subsequent tests don't fail
        }
    }

    @After
    public void stopServer() {
        instance.stop();
    }

    private String doGet(String path) throws IOException {
        String uri = "http://localhost:" + port + path;
        HttpGet get = new HttpGet(uri);
        HttpResponse response = client.execute(get);
        return EntityUtils.toString(response.getEntity());
    }

    private void doPost(String path, byte[] bytes) throws IOException {
        HttpPost post = new HttpPost("http://localhost:" + port + path);
        BasicHttpEntity httpEntity = new BasicHttpEntity();
        httpEntity.setContent(new ByteArrayInputStream(bytes));
        post.setEntity(httpEntity);
        client.execute(post);
    }

}
