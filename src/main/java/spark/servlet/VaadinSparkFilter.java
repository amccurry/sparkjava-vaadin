package spark.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import spark.ExceptionMapper;
import spark.globalstate.ServletFlag;
import spark.http.matching.VaadinMatcherFilter;
import spark.route.ServletRoutes;
import spark.staticfiles.StaticFilesConfiguration;

@Slf4j
@WebFilter(urlPatterns = "/*")
public class VaadinSparkFilter implements Filter {

  public static final String SPARK_APP_ID = "sparkAppId";

  private String filterPath;
  private VaadinMatcherFilter matcherFilter;
  private SparkApplication[] applications;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    ServletFlag.runFromServlet();
    applications = (SparkApplication[]) filterConfig.getServletContext().getAttribute(SPARK_APP_ID);
    if (applications != null) {
      for (SparkApplication application : applications) {
        application.init();
      }
    }
    filterPath = FilterTools.getFilterPath(filterConfig);
    matcherFilter = new VaadinMatcherFilter(ServletRoutes.get(), StaticFilesConfiguration.servletInstance,
        ExceptionMapper.getServletInstance(), true, false);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    final String relativePath = FilterTools.getRelativePath(httpRequest, filterPath);
    if (log.isDebugEnabled()) {
      log.debug(relativePath);
    }
    HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(httpRequest) {
      @Override
      public String getPathInfo() {
        return relativePath;
      }

      @Override
      public String getRequestURI() {
        return relativePath;
      }
    };

    // handle static resources
    boolean consumed = StaticFilesConfiguration.servletInstance.consume(httpRequest, httpResponse);
    if (consumed) {
      return;
    }
    matcherFilter.doFilter(requestWrapper, response, chain);
  }

  @Override
  public void destroy() {
    if (applications != null) {
      for (SparkApplication sparkApplication : applications) {
        sparkApplication.destroy();
      }
    }
  }

}
