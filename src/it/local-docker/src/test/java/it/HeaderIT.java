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

import java.io.IOException;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class HeaderIT {

    @Test
    public void unprocessedRequest() throws IOException {
        String response = RequestUtils.responseText("/headers");
        assertThat(response, containsString("secure: false"));
        assertThat(response, containsString("serverPort: 8080"));
    }

    @Test
    public void appengineHttpRequest() throws IOException {
        String response = RequestUtils.responseText("/headers", request -> {
            request.setRequestProperty("X-AppEngine-Request-ID-Hash", "foobar");
        });
        assertThat(response, containsString("secure: false"));
        assertThat(response, containsString("serverPort: 80"));
        assertThat(response, containsString("scheme: http"));
    }

    @Test
    public void appengineHttpsRequest() throws IOException {
        String response = RequestUtils.responseText("/headers", request -> {
            request.setRequestProperty("X-AppEngine-Request-ID-Hash", "foobar");
            request.setRequestProperty("X-AppEngine-HTTPS", "on");
        });
        assertThat(response, containsString("secure: true"));
        assertThat(response, containsString("serverPort: 443"));
        assertThat(response, containsString("scheme: https"));
    }

    @Test
    public void userIpOverride() throws IOException {
        String response = RequestUtils.responseText("/headers", request -> {
            request.setRequestProperty("X-AppEngine-Request-ID-Hash", "foobar");
            request.setRequestProperty("X-AppEngine-User-IP", "1.2.3.4");
        });
        assertThat(response, containsString("remoteAddr: 1.2.3.4"));
        assertThat(response, containsString("remoteHost: 1.2.3.4"));
    }
}
