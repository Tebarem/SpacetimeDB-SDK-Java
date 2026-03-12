package eu.jlavocat.spacetimedb;

import eu.jlavocat.spacetimedb.bsatn.BsatnReader;
import eu.jlavocat.spacetimedb.messages.server.BsatnRowList;
import eu.jlavocat.spacetimedb.messages.server.RowSizeHint;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class TableCache<T> {

    private final String tableName;
    private final RowDecoder<T> decoder;
    private final Function<T, Object> primaryKeyExtractor;
    private final Map<Object, T> rowsByKey = new ConcurrentHashMap<>();

    private final List<Consumer<T>> insertCallbacks = new CopyOnWriteArrayList<>();
    private final List<Consumer<T>> deleteCallbacks = new CopyOnWriteArrayList<>();

    public TableCache(String tableName, RowDecoder<T> decoder, Function<T, Object> primaryKeyExtractor) {
        this.tableName = tableName;
        this.decoder = decoder;
        this.primaryKeyExtractor = primaryKeyExtractor;
    }

    public String tableName() {
        return tableName;
    }

    public void applyInserts(BsatnRowList rowList) {
        iterateRows(rowList, row -> {
            rowsByKey.put(pk(row), row);
            insertCallbacks.forEach(cb -> cb.accept(row));
        });
    }

    public void applyDeletes(BsatnRowList rowList) {
        iterateRows(rowList, row -> {
            rowsByKey.remove(pk(row));
            deleteCallbacks.forEach(cb -> cb.accept(row));
        });
    }

    public Collection<T> all() {
        return Collections.unmodifiableCollection(rowsByKey.values());
    }

    public Optional<T> findByKey(Object key) {
        return Optional.ofNullable(rowsByKey.get(key));
    }

    public TableCache<T> onInsert(Consumer<T> cb) {
        insertCallbacks.add(cb);
        return this;
    }

    public TableCache<T> onDelete(Consumer<T> cb) {
        deleteCallbacks.add(cb);
        return this;
    }

    private Object pk(T row) {
        return primaryKeyExtractor != null
                ? primaryKeyExtractor.apply(row)
                : System.identityHashCode(row);
    }

    private void iterateRows(BsatnRowList rowList, Consumer<T> action) {
        byte[] data = rowList.rowsData();
        if (data.length == 0) return;

        BsatnReader reader = new BsatnReader(ByteBuffer.wrap(data));

        switch (rowList.rowSizeHint()) {
            case RowSizeHint.FixedSize(int size) -> {
                if (size <= 0) return;
                int numRows = data.length / size;
                for (int i = 0; i < numRows; i++) {
                    reader.position(i * size);
                    action.accept(decoder.decode(reader));
                }
            }
            case RowSizeHint.RowOffsets(long[] offsets) -> {
                for (long offset : offsets) {
                    reader.position((int) offset);
                    action.accept(decoder.decode(reader));
                }
            }
        }
    }
}
