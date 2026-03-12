package eu.jlavocat.spacetimedb.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.*;

public class SchemaParsingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parsePrimitiveU64() throws Exception {
        AlgebraicType type = mapper.readValue("{\"U64\":{}}", AlgebraicType.class);
        assertTrue(type instanceof AlgebraicType.U64);
    }

    @Test
    public void parsePrimitiveString() throws Exception {
        AlgebraicType type = mapper.readValue("{\"String\":{}}", AlgebraicType.class);
        assertTrue(type instanceof AlgebraicType.StringType);
    }

    @Test
    public void parseRef() throws Exception {
        AlgebraicType type = mapper.readValue("{\"Ref\":3}", AlgebraicType.class);
        assertTrue(type instanceof AlgebraicType.RefType);
        assertEquals(3, ((AlgebraicType.RefType) type).ref());
    }

    @Test
    public void parseProduct() throws Exception {
        String json = "{\"Product\":{\"elements\":[{\"name\":\"id\",\"algebraic_type\":{\"U64\":{}}}]}}";
        AlgebraicType type = mapper.readValue(json, AlgebraicType.class);
        assertTrue(type instanceof AlgebraicType.ProductType);
        AlgebraicType.ProductType product = (AlgebraicType.ProductType) type;
        assertEquals(1, product.elements().size());
        assertEquals("id", product.elements().get(0).name());
        assertTrue(product.elements().get(0).algebraicType() instanceof AlgebraicType.U64);
    }

    @Test
    public void parseArray() throws Exception {
        AlgebraicType type = mapper.readValue("{\"Array\":{\"elem_ty\":{\"I32\":{}}}}", AlgebraicType.class);
        assertTrue(type instanceof AlgebraicType.ArrayType);
        AlgebraicType.ArrayType array = (AlgebraicType.ArrayType) type;
        assertTrue(array.elemTy() instanceof AlgebraicType.I32);
    }

    @Test
    public void parseModuleSchema() throws Exception {
        String json = """
            {
              "typespace": {
                "types": [
                  {"ty": {"Product": {"elements": [
                    {"name":"entity_id","algebraic_type":{"U64":{}}},
                    {"name":"name","algebraic_type":{"String":{}}}
                  ]}}}
                ]
              },
              "tables": [{"name":"Player","product_type_ref":0,"primary_key":[0],"table_type":"User","table_access":"Public"}],
              "reducers": [{"name":"set_name","params":{"elements":[]},"lifecycle":null}],
              "types": [{"name":{"name":"Player","scope":[]},"ty":0}]
            }
            """;

        ModuleSchema schema = mapper.readValue(json, ModuleSchema.class);

        assertEquals(1, schema.typespace().types().size());
        assertTrue(schema.typespace().types().get(0).ty() instanceof AlgebraicType.ProductType);

        assertEquals(1, schema.tables().size());
        assertEquals("Player", schema.tables().get(0).name());

        assertEquals(1, schema.reducers().size());
        assertEquals("set_name", schema.reducers().get(0).name());

        assertEquals(1, schema.types().size());
        assertEquals("Player", schema.types().get(0).name().name());
        assertEquals(0, schema.types().get(0).ty());
    }
}
