package spark.vaadin;

import java.lang.reflect.Field;
import java.util.Map;

import lombok.SneakyThrows;
import spark.ExceptionMapper;
import spark.Service;
import spark.embeddedserver.EmbeddedServer;
import spark.embeddedserver.EmbeddedServerFactory;
import spark.embeddedserver.EmbeddedServers;
import spark.embeddedserver.EmbeddedServers.Identifiers;
import spark.embeddedserver.jetty.VaadinEmbeddedJettyFactory;
import spark.route.Routes;
import spark.staticfiles.StaticFilesConfiguration;

public class VaadinSparkService {

  private static final String FACTORIES = "factories";
  private static final String STATIC_FILES_CONFIGURATION = "staticFilesConfiguration";
  private static VaadinEmbeddedJettyFactory factory = new VaadinEmbeddedJettyFactory();

  public static Service ignite() {
    Identifiers defaultIdentifier = EmbeddedServers.defaultIdentifier();
    EmbeddedServers.initialize();
    EmbeddedServerFactory original = getOriginal(defaultIdentifier);
    Service service = Service.ignite();
    StaticFilesConfiguration staticFilesConfigurationRef = getStaticFilesConfiguration(service);
    EmbeddedServers.add(defaultIdentifier, new EmbeddedServerFactory() {
      @Override
      public EmbeddedServer create(Routes routeMatcher, StaticFilesConfiguration staticFilesConfiguration,
          ExceptionMapper exceptionMapper, boolean hasMultipleHandler) {
        if (staticFilesConfigurationRef == staticFilesConfiguration) {
          return factory.create(routeMatcher, staticFilesConfiguration, exceptionMapper, hasMultipleHandler);
        } else {
          return original.create(routeMatcher, staticFilesConfiguration, exceptionMapper, hasMultipleHandler);
        }
      }
    });
    return service;
  }

  @SneakyThrows
  private static StaticFilesConfiguration getStaticFilesConfiguration(Service service) {
    Field declaredField = Service.class.getDeclaredField(STATIC_FILES_CONFIGURATION);
    declaredField.setAccessible(true);
    return (StaticFilesConfiguration) declaredField.get(service);
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private static EmbeddedServerFactory getOriginal(Identifiers defaultIdentifier) {
    Field declaredField = EmbeddedServers.class.getDeclaredField(FACTORIES);
    declaredField.setAccessible(true);
    Map<Object, EmbeddedServerFactory> map = (Map<Object, EmbeddedServerFactory>) declaredField.get(null);
    return map.get(defaultIdentifier);
  }

}
