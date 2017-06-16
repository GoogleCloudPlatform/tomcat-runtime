package com.google.cloud.runtimes.tomcat.test.simple;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Reference the custom tests for the integration framework
 * (https://github.com/GoogleCloudPlatform/runtimes-common/tree/master/integration_tests)
 */
@WebServlet(urlPatterns = {"/custom"})
public class CustomServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    StringBuilder configuration = new StringBuilder();
    configuration.append("[");
    configuration.append("{\"path\": \"custom/tests/secure\"},");
    // Dump the server configuration into the test logs
    configuration.append("{\"path\": \"dump/all\"}");
    configuration.append("]");

    resp.setContentType("application/json");
    resp.getWriter().print(configuration.toString());
  }
}
