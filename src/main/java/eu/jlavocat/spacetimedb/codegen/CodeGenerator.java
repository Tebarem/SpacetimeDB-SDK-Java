package eu.jlavocat.spacetimedb.codegen;

import eu.jlavocat.spacetimedb.schema.ModuleSchema;
import eu.jlavocat.spacetimedb.schema.TypeExport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class CodeGenerator {

    private final ModuleSchema schema;
    private final String outputDir;
    private final String outputPackage;
    final Map<Integer, String> indexToName = new HashMap<>();

    public CodeGenerator(ModuleSchema schema, String outputDir, String outputPackage) {
        this.schema = schema;
        this.outputDir = outputDir;
        this.outputPackage = outputPackage;

        for (TypeExport te : schema.types()) {
            indexToName.put(te.ty(), JavaNameUtils.toClassName(te.name().name()));
        }
    }

    public void generate() throws IOException {
        Path pkgDir = Path.of(outputDir, outputPackage.replace('.', '/'));
        Files.createDirectories(pkgDir);

        new TypeGenerator(schema, pkgDir, outputPackage, indexToName).generate();
        new TableGenerator(schema, pkgDir, outputPackage, indexToName).generate();
        new ReducerGenerator(schema, pkgDir, outputPackage, indexToName).generate();
    }
}
