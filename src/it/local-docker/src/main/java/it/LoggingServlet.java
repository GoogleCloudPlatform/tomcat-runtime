/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package it;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests application logs messages are logged correctly.
 */
@WebServlet("/logging")
public class LoggingServlet extends HttpServlet {
    private static final String NAME = LoggingServlet.class.getName();
    private static final Logger LOG = Logger.getLogger(NAME);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.entering(NAME, "doGet");
        LOG.fine("FINE level message");
        LOG.info("INFO level message");
        LOG.warning("WARNING level message");
        LOG.severe("SEVERE level message");

        LOG.log(Level.WARNING, "WARNING level message with exception", new Throwable().fillInStackTrace());

        getServletContext().log("Servlet log message");
        getServletContext().log("Servlet log message with exception", new Throwable().fillInStackTrace());
        LOG.exiting(NAME, "doGet");
    }
}
