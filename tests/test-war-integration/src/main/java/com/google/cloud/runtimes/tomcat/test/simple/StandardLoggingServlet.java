package com.google.cloud.runtimes.tomcat.test.simple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/logging_standard")
public class StandardLoggingServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(StandardLoggingServlet.class.getName());

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode body = objectMapper.readTree(req.getReader());
    String token = body.path("token").asText();
    String level = body.path("level").asText();

    if (level.equals("DEBUG")) {
      level = "FINE";
    } else if ( level.equals("ERROR") ||
                level.equals("CRITICAL") ||
                level.equals("ALERT")) {
      level = "SEVERE";
    }


    logger.log(Level.parse(level), token);
    resp.setContentType("text/plain");
    resp.getWriter().println(URLEncoder.encode("appengine.googleapis.com/stdout", "UTF-8"));
  }
}
