package spark.embeddedserver.jetty;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.embeddedserver.jetty.websocket.WebSocketHandlerWrapper;
import spark.embeddedserver.jetty.websocket.WebSocketServletContextHandlerFactory;
import spark.ssl.SslStores;

public class VaadinEmbeddedJettyServer extends EmbeddedJettyServer {

  private static final int SPARK_DEFAULT_PORT = 4567;
  private static final String NAME = "Spark";

  private final JettyServerFactory serverFactory;
  private final Handler handler;
  private Server server;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Map<String, WebSocketHandlerWrapper> webSocketHandlers;
  private Optional<Integer> webSocketIdleTimeoutMillis;

  private ThreadPool threadPool = null;

  public VaadinEmbeddedJettyServer(JettyServerFactory serverFactory, Handler handler) {
    super(serverFactory, handler);
    this.serverFactory = serverFactory;
    this.handler = handler;
  }

  @Override
  public void configureWebSockets(Map<String, WebSocketHandlerWrapper> webSocketHandlers,
      Optional<Integer> webSocketIdleTimeoutMillis) {
    this.webSocketHandlers = webSocketHandlers;
    this.webSocketIdleTimeoutMillis = webSocketIdleTimeoutMillis;
  }

  /**
   * {@inheritDoc}
   */
  @Override

  public int ignite(String host, int port, SslStores sslStores, int maxThreads, int minThreads,
      int threadIdleTimeoutMillis) throws Exception {
    boolean hasCustomizedConnectors = false;
    if (port == 0) {
      try (ServerSocket s = new ServerSocket(0)) {
        port = s.getLocalPort();
      } catch (IOException e) {
        logger.error("Could not get first available port (port set to 0), using default: {}", SPARK_DEFAULT_PORT);
        port = SPARK_DEFAULT_PORT;
      }
    }

    // Create instance of jetty server with either default or supplied queued
    // thread pool
    if (threadPool == null) {
      server = serverFactory.create(maxThreads, minThreads, threadIdleTimeoutMillis);
    } else {
      server = serverFactory.create(threadPool);
    }

    ServerConnector connector;

    if (sslStores == null) {
      connector = SocketConnectorFactory.createSocketConnector(server, host, port);
    } else {
      connector = SocketConnectorFactory.createSecureSocketConnector(server, host, port, sslStores);
    }

    Connector previousConnectors[] = server.getConnectors();
    server = connector.getServer();
    if (previousConnectors.length != 0) {
      server.setConnectors(previousConnectors);
      hasCustomizedConnectors = true;
    } else {
      server.setConnectors(new Connector[] { connector });
    }

    ServletContextHandler webSocketServletContextHandler = WebSocketServletContextHandlerFactory
        .create(webSocketHandlers, webSocketIdleTimeoutMillis);

    List<Handler> handlersInList = new ArrayList<>();
    HandlerList handlers = new HandlerList();
    handlersInList.add(handler);
    if (webSocketServletContextHandler != null) {
      handlersInList.add(webSocketServletContextHandler);
    }
    handlersInList.add(VaadinUtil.getHandler("/"));
    handlers.setHandlers(handlersInList.toArray(new Handler[handlersInList.size()]));
    server.setHandler(handlers);
    logger.info("== {} has ignited ...", NAME);
    if (hasCustomizedConnectors) {
      logger.info(">> Listening on Custom Server ports!");
    } else {
      logger.info(">> Listening on {}:{}", host, port);
    }
    server.start();
    return port;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void join() throws InterruptedException {
    server.join();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void extinguish() {
    logger.info(">>> {} shutting down ...", NAME);
    try {
      if (server != null) {
        server.stop();
      }
    } catch (Exception e) {
      logger.error("stop failed", e);
      System.exit(100); // NOSONAR
    }
    logger.info("done");
  }

  @Override
  public int activeThreadCount() {
    if (server == null) {
      return 0;
    }
    return server.getThreadPool().getThreads() - server.getThreadPool().getIdleThreads();
  }

  public EmbeddedJettyServer withThreadPool(ThreadPool threadPool) {
    this.threadPool = threadPool;
    return this;
  }
}
