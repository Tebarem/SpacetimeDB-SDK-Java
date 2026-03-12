package eu.jlavocat.spacetimedb.codegen;

import eu.jlavocat.spacetimedb.schema.AlgebraicType;
import eu.jlavocat.spacetimedb.schema.ModuleSchema;
import eu.jlavocat.spacetimedb.schema.ProductElement;
import eu.jlavocat.spacetimedb.schema.ReducerDef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ReducerGenerator {

    private final ModuleSchema schema;
    private final Path pkgDir;
    private final String pkg;
    private final Map<Integer, String> indexToName;

    public ReducerGenerator(ModuleSchema schema, Path pkgDir, String pkg,
                            Map<Integer, String> indexToName) {
        this.schema = schema;
        this.pkgDir = pkgDir;
        this.pkg = pkg;
        this.indexToName = indexToName;
    }

    public void generate() throws IOException {
        StringBuilder methods = new StringBuilder();

        for (ReducerDef reducer : schema.reducers()) {
            // Skip lifecycle reducers (init, connect, disconnect)
            if (reducer.lifecycle() != null) continue;

            String methodName = JavaNameUtils.toMethodName(reducer.name());
            List<ProductElement> params = reducer.params() != null
                    ? reducer.params().elements()
                    : List.of();

            String paramList = params.stream()
                    .map(e -> JavaNameUtils.javaType(e.algebraicType(), indexToName)
                            + " " + JavaNameUtils.toMethodName(e.name()))
                    .collect(Collectors.joining(", "));

            StringBuilder body = new StringBuilder();
            body.append("        eu.jlavocat.spacetimedb.bsatn.BsatnWriter w = " +
                    "new eu.jlavocat.spacetimedb.bsatn.BsatnWriter();\n");
            for (ProductElement e : params) {
                String fieldName = JavaNameUtils.toMethodName(e.name());
                body.append("        ")
                        .append(JavaNameUtils.writeStmt(e.algebraicType(), indexToName, "w", fieldName))
                        .append("\n");
            }
            body.append("        conn.callReducer(\"").append(reducer.name())
                    .append("\", w.toByteArray());\n");

            methods.append("\n    public void ").append(methodName)
                    .append("(").append(paramList).append(") {\n")
                    .append(body)
                    .append("    }\n");
        }

        String source = "package " + pkg + ";\n\n" +
                "import eu.jlavocat.spacetimedb.DbConnectionImpl;\n\n" +
                "public final class Reducers {\n\n" +
                "    private final DbConnectionImpl conn;\n\n" +
                "    public Reducers(DbConnectionImpl conn) {\n" +
                "        this.conn = conn;\n" +
                "    }\n" +
                methods +
                "}\n";

        Files.writeString(pkgDir.resolve("Reducers.java"), source);
    }
}
