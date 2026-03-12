package eu.jlavocat.spacetimedb.codegen;

import eu.jlavocat.spacetimedb.schema.AlgebraicType;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class JavaNameUtilsTest {

    @Test
    public void toCamelCase_snakeCase() {
        assertEquals("setName", JavaNameUtils.toCamelCase("set_name"));
    }

    @Test
    public void toCamelCase_alreadyCamel() {
        assertEquals("entityId", JavaNameUtils.toCamelCase("entityId"));
    }

    @Test
    public void toClassName() {
        assertEquals("PlayerComponent", JavaNameUtils.toClassName("player_component"));
    }

    @Test
    public void toMethodName() {
        assertEquals("entityId", JavaNameUtils.toMethodName("EntityId"));
    }

    @Test
    public void javaType_primitives() {
        Map<Integer, String> empty = Map.of();
        assertEquals("long",    JavaNameUtils.javaType(new AlgebraicType.U64(), empty));
        assertEquals("String",  JavaNameUtils.javaType(new AlgebraicType.StringType(), empty));
        assertEquals("boolean", JavaNameUtils.javaType(new AlgebraicType.Bool(), empty));
    }

    @Test
    public void javaType_array() {
        Map<Integer, String> empty = Map.of();
        AlgebraicType.ArrayType arr = new AlgebraicType.ArrayType(new AlgebraicType.U64());
        assertEquals("java.util.List<Long>", JavaNameUtils.javaType(arr, empty));
    }

    @Test
    public void boxedJavaType_long() {
        Map<Integer, String> empty = Map.of();
        assertEquals("Integer", JavaNameUtils.boxedJavaType(new AlgebraicType.I32(), empty));
    }

    @Test
    public void readExpr_primitive() {
        Map<Integer, String> empty = Map.of();
        String[] result = JavaNameUtils.readExpr(new AlgebraicType.U64(), empty, "reader", "id");
        assertEquals("", result[0]);
        assertEquals("reader.readU64()", result[1]);
    }

    @Test
    public void readExpr_ref() {
        Map<Integer, String> indexToName = Map.of(0, "Player");
        String[] result = JavaNameUtils.readExpr(new AlgebraicType.RefType(0), indexToName, "reader", "p");
        assertEquals("", result[0]);
        assertEquals("Player.fromBsatn(reader)", result[1]);
    }

    @Test
    public void writeStmt_primitive() {
        Map<Integer, String> empty = Map.of();
        String stmt = JavaNameUtils.writeStmt(new AlgebraicType.StringType(), empty, "w", "name");
        assertEquals("w.writeString(name);", stmt);
    }
}
