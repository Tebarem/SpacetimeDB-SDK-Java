package eu.jlavocat.spacetimedb.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TypeEntry(@JsonProperty("ty") AlgebraicType ty) {}
