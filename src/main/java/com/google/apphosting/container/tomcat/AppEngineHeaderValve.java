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
package com.google.apphosting.container.tomcat;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

/**
 * A Valve that processes App Engine specific headers that were added to the request.
 *
 * TODO: Some of this might be better handled in the HttpProcessor so that it is set up before
 * the request is mapped and dispatched.
 */
public class AppEngineHeaderValve extends ValveBase {
    private static final String X_APPENGINE_REQUEST_ID = "X-AppEngine-Request-ID-Hash";
    private static final String X_APPENGINE_HTTPS = "X-AppEngine-HTTPS";
    private static final String X_APPENGINE_USER_IP = "X-AppEngine-User-IP";

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (isAppEngineRequest(request)) {
            // Check for https:
            if ("on".equalsIgnoreCase(request.getHeader(X_APPENGINE_HTTPS))) {
                request.setSecure(true);
                request.setServerPort(443);
                request.getCoyoteRequest().scheme().setString("https");
            } else {
                request.setSecure(false);
                request.setServerPort(80);
                request.getCoyoteRequest().scheme().setString("http");
            }

            // Set remote user's original IP address.
            String userIP = request.getHeader(X_APPENGINE_USER_IP);
            if (userIP != null) {
                request.setRemoteAddr(userIP);
                request.setRemoteHost(userIP);
            }
        }

        getNext().invoke(request, response);
    }

    private boolean isAppEngineRequest(Request request) {
        return request.getHeader(X_APPENGINE_REQUEST_ID) != null;
    }
}
