package eu.jlavocat.spacetimedb;

import java.io.ByteArrayOutputStream;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import eu.jlavocat.spacetimedb.bsatn.BsatnReader;
import eu.jlavocat.spacetimedb.events.OnConnectedEvent;
import eu.jlavocat.spacetimedb.events.OnDisconnectedEvent;
import eu.jlavocat.spacetimedb.messages.server.DatabaseUpdate;
import eu.jlavocat.spacetimedb.messages.server.IdentityToken;
import eu.jlavocat.spacetimedb.messages.server.ServerMessage;
import eu.jlavocat.spacetimedb.messages.server.UpdateStatus;

public final class WsListener implements WebSocket.Listener {
    private final Optional<Consumer<OnConnectedEvent>> onConnect;
    private final Optional<Consumer<OnDisconnectedEvent>> onDisconnect;
    private final Consumer<IdentityToken> onIdentityToken;
    private final Consumer<DatabaseUpdate> onDatabaseUpdate;
    private final Optional<Runnable> reconnectCallback;

    private ByteArrayOutputStream fragmentedMessageBuffer = null;

    public WsListener(Optional<Consumer<OnConnectedEvent>> onConnect,
            Optional<Consumer<OnDisconnectedEvent>> onDisconnect,
            Consumer<IdentityToken> onIdentityToken,
            Consumer<DatabaseUpdate> onDatabaseUpdate,
            Optional<Runnable> reconnectCallback) {
        this.onConnect = onConnect;
        this.onDisconnect = onDisconnect;
        this.onIdentityToken = onIdentityToken;
        this.onDatabaseUpdate = onDatabaseUpdate;
        this.reconnectCallback = reconnectCallback;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer buffer, boolean last) {
        Optional<ByteBuffer> fullMessage = accumulateBinaryMessage(buffer, last);
        if (fullMessage.isEmpty()) {
            webSocket.request(1);
            return CompletableFuture.completedStage(null);
        }

        var data = fullMessage.get();

        try {
            BsatnReader reader = new BsatnReader(data);

            byte compressionAlgo = reader.readByte();
            if (compressionAlgo != 0) {
                throw new IllegalStateException(
                        "Unsupported compression algorithm: " + compressionAlgo + ", only 0 (none) is supported");
            }

            var message = ServerMessage.fromBsatn(reader);
            if (message == null) {
                webSocket.request(1);
                return CompletableFuture.completedStage(null);
            }

            if (reader.remaining() != 0) {
                throw new IllegalStateException(
                        "BSATN decode error: message not fully consumed, " + reader.remaining() + " bytes remaining");
            }

            switch (message) {
                case ServerMessage.IdentityTokenMessage(IdentityToken msg) -> {
                    onIdentityToken.accept(msg);
                    OnConnectedEvent event = new OnConnectedEvent(msg.identity(), msg.token(), msg.connectionId());
                    onConnect.ifPresent(consumer -> consumer.accept(event));
                }
                case ServerMessage.TransactionUpdateMessage msg -> {
                    if (msg.payload().status() instanceof UpdateStatus.Committed c) {
                        onDatabaseUpdate.accept(c.update());
                    }
                }
                case ServerMessage.TransactionUpdateLightMessage msg -> {
                    onDatabaseUpdate.accept(msg.update());
                }
                case ServerMessage.SubscriptionErrorMessage msg -> {
                    System.err.println("Received SubscriptionErrorMessage " + msg);
                }
                case ServerMessage.SubscribeMultiAppliedMessage msg -> {
                    onDatabaseUpdate.accept(msg.payload().update());
                }
            }

            return CompletableFuture.completedStage(null);
        } catch (Throwable t) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Error processing message: " + t.getMessage());
            return CompletableFuture.completedStage(null);
        } finally {
            webSocket.request(1);
        }

    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        webSocket.request(1);
        return CompletableFuture.completedStage(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        OnDisconnectedEvent event = new OnDisconnectedEvent(statusCode, reason);
        onDisconnect.ifPresent(consumer -> consumer.accept(event));
        if (statusCode != WebSocket.NORMAL_CLOSURE) {
            reconnectCallback.ifPresent(Runnable::run);
        }
        return CompletableFuture.completedStage(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        System.err.println("[SpacetimeDB] WebSocket error: " + error.getMessage());
        reconnectCallback.ifPresent(Runnable::run);
    }

    private Optional<ByteBuffer> accumulateBinaryMessage(ByteBuffer data, boolean last) {
        if (last && fragmentedMessageBuffer == null) {
            return Optional.of(data);
        }

        if (fragmentedMessageBuffer == null) {
            fragmentedMessageBuffer = new ByteArrayOutputStream(Math.max(256, data.remaining()));
        }

        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        fragmentedMessageBuffer.writeBytes(bytes);

        if (last) {
            ByteBuffer fullMessage = ByteBuffer.wrap(fragmentedMessageBuffer.toByteArray());
            fragmentedMessageBuffer = null;
            return Optional.of(fullMessage);
        } else {
            return Optional.empty();
        }
    }

}
