package eu.jlavocat.spacetimedb.schema;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class SchemaFetcher {

    private SchemaFetcher() {}

    public static ModuleSchema fetch(String baseUri, String moduleName, String token)
            throws IOException, InterruptedException {
        String url = baseUri + "/v1/database/" + moduleName + "/schema?version=9";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();

        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch schema: HTTP " + response.statusCode()
                    + " - " + response.body());
        }

        return new ObjectMapper().readValue(response.body(), ModuleSchema.class);
    }
}
