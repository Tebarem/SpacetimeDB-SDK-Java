package eu.jlavocat.spacetimedb.codegen;

import eu.jlavocat.spacetimedb.schema.AlgebraicType;
import eu.jlavocat.spacetimedb.schema.ModuleSchema;
import eu.jlavocat.spacetimedb.schema.TableDef;
import eu.jlavocat.spacetimedb.schema.TypeEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class TableGenerator {

    private final ModuleSchema schema;
    private final Path pkgDir;
    private final String pkg;
    private final Map<Integer, String> indexToName;

    public TableGenerator(ModuleSchema schema, Path pkgDir, String pkg,
                          Map<Integer, String> indexToName) {
        this.schema = schema;
        this.pkgDir = pkgDir;
        this.pkg = pkg;
        this.indexToName = indexToName;
    }

    public void generate() throws IOException {
        List<TypeEntry> typespace = schema.typespace().types();
        for (TableDef table : schema.tables()) {
            String rowClass = JavaNameUtils.toClassName(table.name());
            String tableClass = rowClass + "Table";

            // Determine primary key extractor
            String pkExtractor = "null";
            int ref = table.productTypeRef();
            if (!table.primaryKey().isEmpty() && ref >= 0 && ref < typespace.size()) {
                AlgebraicType at = typespace.get(ref).ty();
                if (at instanceof AlgebraicType.ProductType pt && pt.elements() != null) {
                    int pkIdx = table.primaryKey().get(0);
                    if (pkIdx >= 0 && pkIdx < pt.elements().size()) {
                        String pkField = JavaNameUtils.toMethodName(pt.elements().get(pkIdx).name());
                        pkExtractor = "row -> row." + pkField + "()";
                    }
                }
            }

            String source = "package " + pkg + ";\n\n" +
                    "import eu.jlavocat.spacetimedb.TableCache;\n\n" +
                    "public final class " + tableClass + " extends TableCache<" + rowClass + "> {\n\n" +
                    "    public " + tableClass + "() {\n" +
                    "        super(\"" + table.name() + "\", " + rowClass + "::fromBsatn, " + pkExtractor + ");\n" +
                    "    }\n" +
                    "}\n";

            Files.writeString(pkgDir.resolve(tableClass + ".java"), source);
        }
    }
}
