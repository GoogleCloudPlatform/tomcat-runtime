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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Utilities for getting content from the test server.
 */
public class RequestUtils {

    private static final URI SERVER_URL;

    static {
        SERVER_URL = URI.create(System.getProperty("app.url"));
    }

    /**
     * Verify the server responds.
     */
    public static boolean ping() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) SERVER_URL.toURL().openConnection();
        return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    /**
     * Get content as text.
     */
    public static String responseText(String path) throws IOException {
        return StandardCharsets.UTF_8.decode(responseData(path)).toString();
    }

    /**
     * Get content as text.
     */
    public static String responseText(String path, Consumer<HttpURLConnection> requestPreProcessor) throws IOException {
        return StandardCharsets.UTF_8.decode(responseData(path, requestPreProcessor)).toString();
    }

    /**
     * Get content as a raw byte buffer.
     */
    public static ByteBuffer responseData(String path) throws IOException {
        return responseData(path, t -> {});
    }

    /**
     * Get content as a raw byte buffer, pro-processing the request.
     */
    public static ByteBuffer responseData(String path, Consumer<HttpURLConnection> requestPreProcessor) throws IOException {
        URL url = SERVER_URL.resolve(path).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        requestPreProcessor.accept(connection);
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Got response code: " + connection.getResponseCode());
        }

        try (ReadableByteChannel channel = Channels.newChannel(connection.getInputStream())) {
            ByteBuffer buffer = ByteBuffer.allocate(connection.getContentLength());
            while (buffer.remaining() > 0) {
                channel.read(buffer);
            }
            buffer.flip();
            return buffer;
        }
    }

}
