package eu.jlavocat.spacetimedb.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TableDef(
        String name,
        @JsonProperty("product_type_ref") int productTypeRef,
        @JsonProperty("primary_key")      List<Integer> primaryKey,
        @JsonProperty("table_type")       String tableType,
        @JsonProperty("table_access")     String tableAccess
) {}
