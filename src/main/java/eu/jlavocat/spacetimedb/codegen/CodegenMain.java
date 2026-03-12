package eu.jlavocat.spacetimedb.codegen;

import eu.jlavocat.spacetimedb.schema.ModuleSchema;
import eu.jlavocat.spacetimedb.schema.SchemaFetcher;

public final class CodegenMain {

    public static void main(String[] args) throws Exception {
        String baseUri, moduleName, token, outputDir, outputPackage;

        if (args.length == 5) {
            baseUri       = args[0];
            moduleName    = args[1];
            token         = args[2];
            outputDir     = args[3];
            outputPackage = args[4];
        } else if (args.length == 4) {
            baseUri       = args[0];
            moduleName    = args[1];
            token         = "";
            outputDir     = args[2];
            outputPackage = args[3];
        } else {
            System.err.println("Usage: CodegenMain <baseUri> <moduleName> [token] <outputDir> <outputPackage>");
            System.exit(1);
            return;
        }

        ModuleSchema schema = SchemaFetcher.fetch(baseUri, moduleName, token);
        new CodeGenerator(schema, outputDir, outputPackage).generate();
        System.out.println("Code generation complete.");
    }
}
