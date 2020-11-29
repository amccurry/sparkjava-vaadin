package spark.embeddedserver.jetty;

import java.net.URI;
import java.net.URL;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

import com.vaadin.flow.server.startup.ServletContextListeners;

import lombok.SneakyThrows;

public class VaadinUtil {

  @SneakyThrows
  public static Handler getHandler(String contextPath) {
    URL webRootLocation = VaadinUtil.class.getResource("/META-INF/resources/");
    URI webRootUri = webRootLocation.toURI();
    WebAppContext context = new WebAppContext();
    context.setBaseResource(Resource.newResource(webRootUri));
    context.setContextPath(contextPath);
    context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*.jar|.*/classes/.*");
    context.setConfigurationDiscovered(true);
    context.setConfigurations(new Configuration[] { new AnnotationConfiguration(), new WebInfConfiguration(),
        new WebXmlConfiguration(), new MetaInfConfiguration(), new FragmentConfiguration(), new EnvConfiguration(),
        new PlusConfiguration(), new JettyWebXmlConfiguration() });
    context.getServletContext().setExtendedListenerTypes(true);
    context.addEventListener(new ServletContextListeners());
    return context;
  }

}
