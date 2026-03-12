package eu.jlavocat.spacetimedb;

import eu.jlavocat.spacetimedb.messages.server.DatabaseUpdate;
import eu.jlavocat.spacetimedb.messages.server.QueryUpdate;
import eu.jlavocat.spacetimedb.messages.server.TableUpdate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TableRegistry {

    private final Map<String, TableCache<?>> caches = new ConcurrentHashMap<>();

    public <T> void register(TableCache<T> cache) {
        caches.put(cache.tableName(), cache);
    }

    public void applyDatabaseUpdate(DatabaseUpdate update) {
        for (TableUpdate tu : update.tables()) {
            TableCache<?> cache = caches.get(tu.tableName());
            if (cache == null) continue;
            for (QueryUpdate qu : tu.updates()) {
                cache.applyDeletes(qu.deletes());
                cache.applyInserts(qu.inserts());
            }
        }
    }
}
