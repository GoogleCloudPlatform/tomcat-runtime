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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SmokeIT {

    private static final URI base;
    static {
        base = URI.create(System.getProperty("app.url"));
    }

    @Test
    public void canConnect() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) base.toURL().openConnection();
        assertEquals(200, connection.getResponseCode());
    }

    @Test
    public void canUseJSP() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) base.resolve("/hello.jsp").toURL().openConnection();
        assertEquals(200, connection.getResponseCode());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            reader.readLine();
            assertEquals("Hello JSP", reader.readLine());
        }
    }
}
