package eu.jlavocat.spacetimedb.schema;

public record ReducerDef(
        String name,
        AlgebraicType.ProductType params,
        String lifecycle
) {}
