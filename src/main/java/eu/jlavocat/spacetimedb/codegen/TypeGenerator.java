package eu.jlavocat.spacetimedb.codegen;

import eu.jlavocat.spacetimedb.schema.AlgebraicType;
import eu.jlavocat.spacetimedb.schema.ModuleSchema;
import eu.jlavocat.spacetimedb.schema.ProductElement;
import eu.jlavocat.spacetimedb.schema.SumVariant;
import eu.jlavocat.spacetimedb.schema.TypeEntry;
import eu.jlavocat.spacetimedb.schema.TypeExport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TypeGenerator {

    private final ModuleSchema schema;
    private final Path pkgDir;
    private final String pkg;
    private final Map<Integer, String> indexToName;

    public TypeGenerator(ModuleSchema schema, Path pkgDir, String pkg,
                         Map<Integer, String> indexToName) {
        this.schema = schema;
        this.pkgDir = pkgDir;
        this.pkg = pkg;
        this.indexToName = indexToName;
    }

    public void generate() throws IOException {
        List<TypeEntry> typespace = schema.typespace().types();
        for (TypeExport te : schema.types()) {
            String className = JavaNameUtils.toClassName(te.name().name());
            if (te.ty() < 0 || te.ty() >= typespace.size()) continue;
            AlgebraicType at = typespace.get(te.ty()).ty();
            String source = switch (at) {
                case AlgebraicType.ProductType p -> generateProduct(className, p);
                case AlgebraicType.SumType s     -> generateSum(className, s);
                default                          -> null;
            };
            if (source != null) {
                Files.writeString(pkgDir.resolve(className + ".java"), source);
            }
        }
    }

    // ------------------------------------------------------------------ product

    private String generateProduct(String className, AlgebraicType.ProductType pt) {
        List<ProductElement> elems = pt.elements();
        String fields = elems.stream()
                .map(e -> JavaNameUtils.javaType(e.algebraicType(), indexToName)
                        + " " + JavaNameUtils.toMethodName(e.name()))
                .collect(Collectors.joining(", "));

        StringBuilder fromBody = new StringBuilder();
        for (ProductElement e : elems) {
            String fieldName = JavaNameUtils.toMethodName(e.name());
            String[] read = JavaNameUtils.readExpr(e.algebraicType(), indexToName, "reader", fieldName);
            if (!read[0].isEmpty()) {
                fromBody.append("        ").append(read[0]).append("\n");
                fromBody.append("        ")
                        .append(JavaNameUtils.javaType(e.algebraicType(), indexToName))
                        .append(" ").append(fieldName).append(" = ").append(read[1]).append(";\n");
            } else {
                fromBody.append("        ")
                        .append(JavaNameUtils.javaType(e.algebraicType(), indexToName))
                        .append(" ").append(fieldName)
                        .append(" = ").append(read[1]).append(";\n");
            }
        }
        String returnArgs = elems.stream()
                .map(e -> JavaNameUtils.toMethodName(e.name()))
                .collect(Collectors.joining(", "));
        fromBody.append("        return new ").append(className).append("(").append(returnArgs).append(");");

        StringBuilder toBody = new StringBuilder();
        toBody.append("        eu.jlavocat.spacetimedb.bsatn.BsatnWriter w = " +
                "new eu.jlavocat.spacetimedb.bsatn.BsatnWriter();\n");
        for (ProductElement e : elems) {
            String fieldName = JavaNameUtils.toMethodName(e.name());
            String write = JavaNameUtils.writeStmt(e.algebraicType(), indexToName, "w", fieldName);
            toBody.append("        ").append(write).append("\n");
        }
        toBody.append("        return w.toByteBuffer();");

        return "package " + pkg + ";\n\n" +
                "import eu.jlavocat.spacetimedb.bsatn.BsatnReader;\n" +
                "import eu.jlavocat.spacetimedb.bsatn.ToBsatn;\n" +
                "import java.nio.ByteBuffer;\n\n" +
                "public record " + className + "(" + fields + ") implements ToBsatn {\n\n" +
                "    public static " + className + " fromBsatn(BsatnReader reader) {\n" +
                fromBody + "\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public ByteBuffer toBsatn() {\n" +
                toBody + "\n" +
                "    }\n" +
                "}\n";
    }

    // ------------------------------------------------------------------ sum

    private String generateSum(String className, AlgebraicType.SumType st) {
        List<SumVariant> variants = st.variants();

        StringBuilder permits = new StringBuilder();
        StringBuilder variantDefs = new StringBuilder();
        StringBuilder switchCases = new StringBuilder();

        for (int i = 0; i < variants.size(); i++) {
            SumVariant v = variants.get(i);
            String vName = JavaNameUtils.toClassName(v.name());
            if (i > 0) permits.append(", ");
            permits.append(className).append(".").append(vName);

            // variant fields (empty for unit variants)
            String vFields = "";
            String vFromBody = "        return new " + vName + "();\n";
            String vToBody = "        eu.jlavocat.spacetimedb.bsatn.BsatnWriter w = " +
                    "new eu.jlavocat.spacetimedb.bsatn.BsatnWriter();\n" +
                    "        w.writeU8(" + i + ");\n" +
                    "        return w.toByteBuffer();\n";

            if (v.algebraicType() instanceof AlgebraicType.ProductType pt
                    && pt.elements() != null && !pt.elements().isEmpty()) {
                vFields = pt.elements().stream()
                        .map(e -> JavaNameUtils.javaType(e.algebraicType(), indexToName)
                                + " " + JavaNameUtils.toMethodName(e.name()))
                        .collect(Collectors.joining(", "));

                StringBuilder vFrom = new StringBuilder();
                for (ProductElement e : pt.elements()) {
                    String fn = JavaNameUtils.toMethodName(e.name());
                    String[] read = JavaNameUtils.readExpr(e.algebraicType(), indexToName, "reader", fn);
                    if (!read[0].isEmpty()) {
                        vFrom.append("        ").append(read[0]).append("\n");
                        vFrom.append("        ")
                                .append(JavaNameUtils.javaType(e.algebraicType(), indexToName))
                                .append(" ").append(fn).append(" = ").append(read[1]).append(";\n");
                    } else {
                        vFrom.append("        ")
                                .append(JavaNameUtils.javaType(e.algebraicType(), indexToName))
                                .append(" ").append(fn).append(" = ").append(read[1]).append(";\n");
                    }
                }
                String retArgs = pt.elements().stream()
                        .map(e -> JavaNameUtils.toMethodName(e.name()))
                        .collect(Collectors.joining(", "));
                vFrom.append("        return new ").append(vName).append("(").append(retArgs).append(");\n");
                vFromBody = vFrom.toString();

                StringBuilder vTo = new StringBuilder();
                vTo.append("        eu.jlavocat.spacetimedb.bsatn.BsatnWriter w = " +
                        "new eu.jlavocat.spacetimedb.bsatn.BsatnWriter();\n");
                vTo.append("        w.writeU8(").append(i).append(");\n");
                for (ProductElement e : pt.elements()) {
                    String fn = JavaNameUtils.toMethodName(e.name());
                    vTo.append("        ")
                            .append(JavaNameUtils.writeStmt(e.algebraicType(), indexToName, "w", fn))
                            .append("\n");
                }
                vTo.append("        return w.toByteBuffer();\n");
                vToBody = vTo.toString();
            }

            variantDefs.append("\n    public record ").append(vName)
                    .append("(").append(vFields).append(") implements ").append(className).append(" {\n")
                    .append("        public static ").append(vName)
                    .append(" fromBsatn(eu.jlavocat.spacetimedb.bsatn.BsatnReader reader) {\n")
                    .append(vFromBody.replace("        ", "            "))
                    .append("        }\n")
                    .append("        @Override public java.nio.ByteBuffer toBsatn() {\n")
                    .append(vToBody.replace("        ", "            "))
                    .append("        }\n")
                    .append("    }\n");

            switchCases.append("            case ").append(i).append(" -> ")
                    .append(vName).append(".fromBsatn(reader);\n");
        }

        return "package " + pkg + ";\n\n" +
                "import eu.jlavocat.spacetimedb.bsatn.ToBsatn;\n\n" +
                "public sealed interface " + className + " extends ToBsatn\n" +
                "        permits " + permits + " {\n" +
                variantDefs +
                "\n    static " + className +
                " fromBsatn(eu.jlavocat.spacetimedb.bsatn.BsatnReader reader) {\n" +
                "        int _tag = reader.readU8();\n" +
                "        return switch (_tag) {\n" +
                switchCases +
                "            default -> throw new IllegalStateException(" +
                "\"Unknown variant tag: \" + _tag);\n" +
                "        };\n" +
                "    }\n" +
                "}\n";
    }
}
