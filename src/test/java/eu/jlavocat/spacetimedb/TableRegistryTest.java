package eu.jlavocat.spacetimedb;

import eu.jlavocat.spacetimedb.bsatn.BsatnReader;
import eu.jlavocat.spacetimedb.bsatn.BsatnWriter;
import eu.jlavocat.spacetimedb.messages.server.BsatnRowList;
import eu.jlavocat.spacetimedb.messages.server.DatabaseUpdate;
import eu.jlavocat.spacetimedb.messages.server.QueryUpdate;
import eu.jlavocat.spacetimedb.messages.server.RowSizeHint;
import eu.jlavocat.spacetimedb.messages.server.TableUpdate;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TableRegistryTest {

    record SimpleRow(long id, long score) {
        static SimpleRow fromBsatn(BsatnReader r) {
            return new SimpleRow(r.readU64(), r.readU64());
        }
    }

    private static final int ROW_SIZE = 16;

    private BsatnRowList emptyRowList() {
        return new BsatnRowList(new RowSizeHint.FixedSize(ROW_SIZE), new byte[0]);
    }

    private BsatnRowList singleRow(long id, long score) {
        BsatnWriter w = new BsatnWriter();
        w.writeU64(id); w.writeU64(score);
        return new BsatnRowList(new RowSizeHint.FixedSize(ROW_SIZE), w.toByteArray());
    }

    private DatabaseUpdate makeUpdate(String tableName, BsatnRowList inserts) {
        QueryUpdate qu = new QueryUpdate(emptyRowList(), inserts);
        TableUpdate tu = new TableUpdate(1L, tableName, 1L, List.of(qu));
        return new DatabaseUpdate(List.of(tu));
    }

    @Test
    public void routesToRegisteredCache() {
        TableRegistry registry = new TableRegistry();
        TableCache<SimpleRow> cache = new TableCache<>("TestTable", SimpleRow::fromBsatn, SimpleRow::id);
        registry.register(cache);

        registry.applyDatabaseUpdate(makeUpdate("TestTable", singleRow(42L, 99L)));

        assertEquals(1, cache.all().size());
        SimpleRow row = cache.findByKey(42L).orElseThrow();
        assertEquals(99L, row.score());
    }

    @Test
    public void unknownTableIgnored() {
        TableRegistry registry = new TableRegistry();
        // No cache registered — should not throw
        registry.applyDatabaseUpdate(makeUpdate("UnknownTable", singleRow(1L, 1L)));
    }

    @Test
    public void unregisteredTableNotUpdated() {
        TableRegistry registry = new TableRegistry();
        TableCache<SimpleRow> cacheA = new TableCache<>("TableA", SimpleRow::fromBsatn, SimpleRow::id);
        TableCache<SimpleRow> cacheB = new TableCache<>("TableB", SimpleRow::fromBsatn, SimpleRow::id);
        registry.register(cacheA);
        registry.register(cacheB);

        // Only update TableA
        registry.applyDatabaseUpdate(makeUpdate("TableA", singleRow(1L, 10L)));

        assertEquals(1, cacheA.all().size());
        assertTrue(cacheB.all().isEmpty());
    }
}
