package eu.jlavocat.spacetimedb;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import eu.jlavocat.spacetimedb.bsatn.ConnectionId;
import eu.jlavocat.spacetimedb.bsatn.Identity;
import eu.jlavocat.spacetimedb.events.OnConnectedEvent;
import eu.jlavocat.spacetimedb.events.OnDisconnectedEvent;
import eu.jlavocat.spacetimedb.messages.client.ClientMessage;
import eu.jlavocat.spacetimedb.messages.server.IdentityToken;
import eu.jlavocat.spacetimedb.schema.ModuleSchema;

public class DbConnectionImpl {

    private Websocket ws;
    private boolean lightMode;

    private final String uri;
    private final String moduleName;
    private final Optional<Consumer<OnConnectedEvent>> onConnect;
    private final Optional<Consumer<OnDisconnectedEvent>> onDisconnect;
    private final boolean autoReconnect;
    private final List<String[]> activeSubscriptions = new ArrayList<>();

    private AtomicLong requestIdCounter = new AtomicLong();
    private AtomicLong queryIdCounter = new AtomicLong();

    private Identity identity;
    private String token;
    private ConnectionId connectionId;

    private ModuleSchema schema;
    private final TableRegistry tableRegistry = new TableRegistry();

    public DbConnectionImpl(String uri, String moduleName, String token,
            Optional<Consumer<OnConnectedEvent>> onConnect,
            Optional<Consumer<OnDisconnectedEvent>> onDisconnect, boolean lightMode,
            ModuleSchema schema, boolean autoReconnect) {
        this.uri = uri;
        this.moduleName = moduleName;
        this.token = token;
        this.onConnect = onConnect;
        this.onDisconnect = onDisconnect;
        this.lightMode = lightMode;
        this.schema = schema;
        this.autoReconnect = autoReconnect;
        this.identity = null;

        this.ws = new Websocket(uri, moduleName, token,
                this::onIdentityTokenReceived,
                onConnect,
                onDisconnect,
                tableRegistry::applyDatabaseUpdate,
                autoReconnect ? Optional.of(this::scheduleReconnect) : Optional.empty());
    }

    private void onIdentityTokenReceived(IdentityToken msg) {
        this.identity = msg.identity();
        this.token = msg.token();
        this.connectionId = msg.connectionId();
        for (String[] queries : activeSubscriptions) {
            sendSubscription(queries);
        }
    }

    public void reconnect() {
        this.ws = new Websocket(uri, moduleName, token,
                this::onIdentityTokenReceived,
                onConnect,
                onDisconnect,
                tableRegistry::applyDatabaseUpdate,
                autoReconnect ? Optional.of(this::scheduleReconnect) : Optional.empty());
    }

    private void scheduleReconnect() {
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS)
                .execute(this::reconnect);
    }

    public Identity identity() {
        return identity;
    }

    public String token() {
        return token;
    }

    public ConnectionId connectionId() {
        return connectionId;
    }

    public ModuleSchema schema() {
        return schema;
    }

    public <T> DbConnectionImpl registerTable(TableCache<T> cache) {
        tableRegistry.register(cache);
        return this;
    }

    public void callReducer(String reducerName, byte[] args) {
        long requestId = requestIdCounter.incrementAndGet();

        // TODO: Enqueue the message and send them in another part of the code to reuse
        // a single buffer for multiple messages, reducing allocations.

        ws.send(new ClientMessage.CallReducerMessage(reducerName, args, requestId, lightMode).toBsatn());
    }

    public void subscribe(String[] queries) {
        activeSubscriptions.add(queries);
        sendSubscription(queries);
    }

    private void sendSubscription(String[] queries) {
        long requestId = requestIdCounter.incrementAndGet();
        long queryId = queryIdCounter.incrementAndGet();

        // TODO: Enqueue the message and send them in another part of the code to reuse
        // a single buffer for multiple messages, reducing allocations.

        ws.send(new ClientMessage.SubscribeMultiMessage(queries, requestId, queryId).toBsatn());
    }

}
