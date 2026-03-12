package eu.jlavocat.spacetimedb;

import eu.jlavocat.spacetimedb.bsatn.BsatnReader;
import eu.jlavocat.spacetimedb.bsatn.BsatnWriter;
import eu.jlavocat.spacetimedb.messages.server.BsatnRowList;
import eu.jlavocat.spacetimedb.messages.server.RowSizeHint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TableCacheTest {

    // --- Layout A helpers: two U64 fields (id, score), 16 bytes/row ---

    record SimpleRow(long id, long score) {
        static SimpleRow fromBsatn(BsatnReader r) {
            return new SimpleRow(r.readU64(), r.readU64());
        }
    }

    private TableCache<SimpleRow> newSimpleCache() {
        return new TableCache<>("Simple", SimpleRow::fromBsatn, SimpleRow::id);
    }

    private BsatnRowList twoSimpleRows() {
        BsatnWriter w = new BsatnWriter();
        w.writeU64(1L); w.writeU64(100L);
        w.writeU64(2L); w.writeU64(200L);
        return new BsatnRowList(new RowSizeHint.FixedSize(16), w.toByteArray());
    }

    // --- Layout B helpers: U64 + String (variable-size rows) ---

    record NamedRow(long id, String name) {
        static NamedRow fromBsatn(BsatnReader r) {
            return new NamedRow(r.readU64(), r.readString());
        }
    }

    private TableCache<NamedRow> newNamedCache() {
        return new TableCache<>("Named", NamedRow::fromBsatn, NamedRow::id);
    }

    private BsatnRowList twoNamedRows() {
        BsatnWriter w = new BsatnWriter();
        // row 0 at offset 0: id=1, name="Alice"  → 8 + 4 + 5 = 17 bytes
        w.writeU64(1L); w.writeString("Alice");
        // row 1 at offset 17: id=2, name="Bob"   → 8 + 4 + 3 = 15 bytes
        w.writeU64(2L); w.writeString("Bob");
        return new BsatnRowList(new RowSizeHint.RowOffsets(new long[]{0L, 17L}), w.toByteArray());
    }

    // ==================== Fixed-size tests ====================

    @Test
    public void applyInsertsFixedSize_populatesCache() {
        TableCache<SimpleRow> cache = newSimpleCache();
        cache.applyInserts(twoSimpleRows());
        assertEquals(2, cache.all().size());

        SimpleRow r1 = cache.findByKey(1L).orElseThrow();
        assertEquals(100L, r1.score());

        SimpleRow r2 = cache.findByKey(2L).orElseThrow();
        assertEquals(200L, r2.score());
    }

    @Test
    public void findByKey_returnsCorrectRow() {
        TableCache<SimpleRow> cache = newSimpleCache();
        cache.applyInserts(twoSimpleRows());
        SimpleRow row = cache.findByKey(1L).orElseThrow();
        assertEquals(100L, row.score());
    }

    @Test
    public void applyDeletes_removesRow() {
        TableCache<SimpleRow> cache = newSimpleCache();
        cache.applyInserts(twoSimpleRows());

        // Delete only row with id=1
        BsatnWriter w = new BsatnWriter();
        w.writeU64(1L); w.writeU64(100L);
        BsatnRowList deleteList = new BsatnRowList(new RowSizeHint.FixedSize(16), w.toByteArray());

        cache.applyDeletes(deleteList);
        assertEquals(1, cache.all().size());
        assertFalse(cache.findByKey(1L).isPresent());
        assertTrue(cache.findByKey(2L).isPresent());
    }

    @Test
    public void onInsert_callbackFired() {
        TableCache<SimpleRow> cache = newSimpleCache();
        List<SimpleRow> fired = new ArrayList<>();
        cache.onInsert(fired::add);
        cache.applyInserts(twoSimpleRows());
        assertEquals(2, fired.size());
    }

    @Test
    public void onDelete_callbackFired() {
        TableCache<SimpleRow> cache = newSimpleCache();
        cache.applyInserts(twoSimpleRows());

        List<SimpleRow> deleted = new ArrayList<>();
        cache.onDelete(deleted::add);

        BsatnWriter w = new BsatnWriter();
        w.writeU64(1L); w.writeU64(100L);
        cache.applyDeletes(new BsatnRowList(new RowSizeHint.FixedSize(16), w.toByteArray()));

        assertEquals(1, deleted.size());
        assertEquals(1L, deleted.get(0).id());
    }

    @Test
    public void emptyRowData_noRows() {
        TableCache<SimpleRow> cache = newSimpleCache();
        cache.applyInserts(new BsatnRowList(new RowSizeHint.FixedSize(16), new byte[0]));
        assertTrue(cache.all().isEmpty());
    }

    // ==================== RowOffsets tests ====================

    @Test
    public void applyInsertsRowOffsets_populatesCache() {
        TableCache<NamedRow> cache = newNamedCache();
        cache.applyInserts(twoNamedRows());
        assertEquals(2, cache.all().size());

        NamedRow alice = cache.findByKey(1L).orElseThrow();
        assertEquals("Alice", alice.name());

        NamedRow bob = cache.findByKey(2L).orElseThrow();
        assertEquals("Bob", bob.name());
    }

    @Test
    public void findByKey_rowOffsets() {
        TableCache<NamedRow> cache = newNamedCache();
        cache.applyInserts(twoNamedRows());
        NamedRow row = cache.findByKey(2L).orElseThrow();
        assertEquals("Bob", row.name());
    }
}
