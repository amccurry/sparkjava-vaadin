package spark.embeddedserver.jetty;

import javax.servlet.FilterConfig;

import org.eclipse.jetty.util.thread.ThreadPool;

import spark.ExceptionMapper;
import spark.embeddedserver.EmbeddedServer;
import spark.embeddedserver.EmbeddedServerFactory;
import spark.http.matching.MatcherFilter;
import spark.route.Routes;
import spark.staticfiles.StaticFilesConfiguration;

public class VaadinEmbeddedJettyFactory implements EmbeddedServerFactory {
  private final JettyServerFactory serverFactory;
  private ThreadPool threadPool;
  private boolean httpOnly = true;

  public VaadinEmbeddedJettyFactory() {
    this.serverFactory = new JettyServer();
  }

  public VaadinEmbeddedJettyFactory(JettyServerFactory serverFactory) {
    this.serverFactory = serverFactory;
  }

  public EmbeddedServer create(Routes routeMatcher, StaticFilesConfiguration staticFilesConfiguration,
      ExceptionMapper exceptionMapper, boolean hasMultipleHandler) {
    // always true for vaadin app
    hasMultipleHandler = true;
    MatcherFilter matcherFilter = new MatcherFilter(routeMatcher, staticFilesConfiguration, exceptionMapper, false,
        hasMultipleHandler);
    matcherFilter.init((FilterConfig) null);
    JettyHandler handler = new JettyHandler(matcherFilter);
    handler.getSessionCookieConfig().setHttpOnly(this.httpOnly);
    return (new VaadinEmbeddedJettyServer(this.serverFactory, handler)).withThreadPool(this.threadPool);
  }

  public VaadinEmbeddedJettyFactory withThreadPool(ThreadPool threadPool) {
    this.threadPool = threadPool;
    return this;
  }

  public VaadinEmbeddedJettyFactory withHttpOnly(boolean httpOnly) {
    this.httpOnly = httpOnly;
    return this;
  }
}