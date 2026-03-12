package eu.jlavocat.spacetimedb;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

import eu.jlavocat.spacetimedb.events.OnConnectedEvent;
import eu.jlavocat.spacetimedb.events.OnDisconnectedEvent;
import eu.jlavocat.spacetimedb.schema.ModuleSchema;
import eu.jlavocat.spacetimedb.schema.SchemaFetcher;

public class DbConnectionBuilder {

    private String uri = "http://localhost:3000";
    private String moduleName = "";
    private String token = "";
    private boolean fetchSchema = false;
    private boolean autoReconnect = false;

    private Optional<Consumer<OnConnectedEvent>> onConnect = Optional.empty();
    private Optional<Consumer<OnDisconnectedEvent>> onDisconnect = Optional.empty();

    public DbConnectionBuilder withUri(String uri) {
        this.uri = uri;
        return this;
    }

    public DbConnectionBuilder withModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    public DbConnectionBuilder withToken(String token) {
        this.token = token;
        return this;
    }

    public DbConnectionBuilder withSchemaFetch(boolean fetchSchema) {
        this.fetchSchema = fetchSchema;
        return this;
    }

    public DbConnectionBuilder onConnect(Consumer<OnConnectedEvent> onConnect) {
        this.onConnect = Optional.of(onConnect);
        return this;
    }

    public DbConnectionBuilder onDisconnect(Consumer<OnDisconnectedEvent> onDisconnect) {
        this.onDisconnect = Optional.of(onDisconnect);
        return this;
    }

    public DbConnectionBuilder withAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
        return this;
    }

    public DbConnectionImpl build() throws IOException, InterruptedException {
        if (uri.isEmpty()) {
            throw new IllegalArgumentException("URI must be provided");
        }

        if (moduleName.isEmpty()) {
            throw new IllegalArgumentException("Module name must be provided");
        }

        ModuleSchema schema = fetchSchema ? SchemaFetcher.fetch(uri, moduleName, token) : null;

        return new DbConnectionImpl(uri, moduleName, token, onConnect, onDisconnect, false, schema, autoReconnect);
    }

}
