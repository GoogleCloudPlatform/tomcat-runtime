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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineHeaderValveTest {

    private org.apache.coyote.Request coyoteRequest;
    private Request request;
    private Response response;

    @Mock
    private Valve next;

    private AppEngineHeaderValve valve = new AppEngineHeaderValve();

    @Before
    public void init() {
        coyoteRequest = new org.apache.coyote.Request();
        coyoteRequest.scheme().setString("http");
        coyoteRequest.setRemotePort(1234);

        request = new Request();
        request.setCoyoteRequest(coyoteRequest);
        request.setSecure(false);
        request.setServerPort(8080);

        response = new Response();

        valve.setNext(next);

        setHeader("x-appengine-request-id-hash", "xxxxxxx");
    }

    @Test
    public void valuesShouldNotBeChangedIfThereAreNoHeaders() throws IOException, ServletException {
        coyoteRequest.getMimeHeaders().clear();
        valve.invoke(request, response);
        verify(next, times(1)).invoke(request, response);
        assertFalse(request.isSecure());
        assertEquals("http", request.getScheme());
        assertEquals(8080, request.getServerPort());
    }

    @Test
    public void secureIsFalse() throws IOException, ServletException {
        setHeader("x-appengine-https", "off");
        valve.invoke(request, response);
        assertFalse(request.isSecure());
        assertEquals("http", request.getScheme());
        assertEquals(80, request.getServerPort());
        verify(next, times(1)).invoke(request, response);
    }

    @Test
    public void secureIsTrue() throws IOException, ServletException {
        setHeader("X-APPENGINE-HTTPS", "on");
        valve.invoke(request, response);
        assertTrue(request.isSecure());
        assertEquals("https", request.getScheme());
        assertEquals(443, request.getServerPort());
        verify(next, times(1)).invoke(request, response);
    }

    @Test
    public void userIpIsSet() throws IOException, ServletException {
        setHeader("x-appengine-user-ip", "1.2.3.4");
        valve.invoke(request, response);
        assertEquals("1.2.3.4", request.getRemoteHost());
        assertEquals("1.2.3.4", request.getRemoteAddr());
        assertEquals(1234, request.getRemotePort());
        verify(next, times(1)).invoke(request, response);
    }

    private void setHeader(String name, String value) {
        coyoteRequest.getMimeHeaders().addValue(name).setString(value);
    }
}
