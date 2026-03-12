package eu.jlavocat.spacetimedb.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SumVariant(
        String name,
        @JsonProperty("algebraic_type") AlgebraicType algebraicType
) {}
