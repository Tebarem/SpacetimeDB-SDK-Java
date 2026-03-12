package eu.jlavocat.spacetimedb.schema;

import java.util.List;

public record ModuleSchema(
        Typespace typespace,
        List<TableDef> tables,
        List<ReducerDef> reducers,
        List<TypeExport> types
) {}
