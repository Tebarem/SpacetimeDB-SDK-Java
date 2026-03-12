package eu.jlavocat.spacetimedb.codegen;

import eu.jlavocat.spacetimedb.schema.AlgebraicType;

import java.util.Map;

public final class JavaNameUtils {

    private JavaNameUtils() {}

    /** Converts snake_case (or any_case) to camelCase. */
    public static String toCamelCase(String name) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                sb.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return sb.toString();
    }

    /** Converts a name to a Java class name (PascalCase). */
    public static String toClassName(String name) {
        String camel = toCamelCase(name);
        if (camel.isEmpty()) return camel;
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    /** Converts a name to a Java method/field name (camelCase, lowercase first char). */
    public static String toMethodName(String name) {
        String camel = toCamelCase(name);
        if (camel.isEmpty()) return camel;
        return Character.toLowerCase(camel.charAt(0)) + camel.substring(1);
    }

    /** Returns the Java type string for an AlgebraicType. */
    public static String javaType(AlgebraicType type, Map<Integer, String> indexToName) {
        return switch (type) {
            case AlgebraicType.Bool b        -> "boolean";
            case AlgebraicType.U8 u          -> "int";
            case AlgebraicType.I8 i          -> "byte";
            case AlgebraicType.U16 u         -> "int";
            case AlgebraicType.I16 i         -> "short";
            case AlgebraicType.U32 u         -> "long";
            case AlgebraicType.I32 i         -> "int";
            case AlgebraicType.U64 u         -> "long";
            case AlgebraicType.I64 i         -> "long";
            case AlgebraicType.U128 u        -> "eu.jlavocat.spacetimedb.bsatn.U128";
            case AlgebraicType.I128 i        -> "eu.jlavocat.spacetimedb.bsatn.I128";
            case AlgebraicType.U256 u        -> "eu.jlavocat.spacetimedb.bsatn.U256";
            case AlgebraicType.I256 i        -> "eu.jlavocat.spacetimedb.bsatn.I256";
            case AlgebraicType.F32 f         -> "float";
            case AlgebraicType.F64 f         -> "double";
            case AlgebraicType.StringType s  -> "String";
            case AlgebraicType.RefType r     -> indexToName.getOrDefault(r.ref(), "Object");
            case AlgebraicType.ArrayType a   ->
                "java.util.List<" + boxedJavaType(a.elemTy(), indexToName) + ">";
            case AlgebraicType.MapType m     ->
                "java.util.Map<" + boxedJavaType(m.keyTy(), indexToName)
                        + ", " + boxedJavaType(m.valueTy(), indexToName) + ">";
            case AlgebraicType.ProductType p -> "Object"; // inline product in field — unusual
            case AlgebraicType.SumType s     -> "Object"; // inline sum in field — unusual
        };
    }

    /** Returns the boxed Java type string (for use in generics). */
    public static String boxedJavaType(AlgebraicType type, Map<Integer, String> indexToName) {
        String raw = javaType(type, indexToName);
        return switch (raw) {
            case "boolean" -> "Boolean";
            case "byte"    -> "Byte";
            case "short"   -> "Short";
            case "int"     -> "Integer";
            case "long"    -> "Long";
            case "float"   -> "Float";
            case "double"  -> "Double";
            default        -> raw;
        };
    }

    /**
     * Returns (preamble, expression) for reading a value from a BsatnReader named {@code readerVar}.
     * preamble may be empty; if non-empty it must be emitted before the expression line.
     * The expression, when combined with preamble, produces a value of javaType(type).
     *
     * @param varName the Java variable name that will hold the decoded value (used for preamble temp vars)
     */
    public static String[] readExpr(AlgebraicType type, Map<Integer, String> indexToName,
                                    String readerVar, String varName) {
        return switch (type) {
            case AlgebraicType.Bool b       -> new String[]{"", readerVar + ".readBool()"};
            case AlgebraicType.U8 u         -> new String[]{"", readerVar + ".readU8()"};
            case AlgebraicType.I8 i         -> new String[]{"", readerVar + ".readI8()"};
            case AlgebraicType.U16 u        -> new String[]{"", readerVar + ".readU16()"};
            case AlgebraicType.I16 i        -> new String[]{"", readerVar + ".readI16()"};
            case AlgebraicType.U32 u        -> new String[]{"", readerVar + ".readU32()"};
            case AlgebraicType.I32 i        -> new String[]{"", readerVar + ".readI32()"};
            case AlgebraicType.U64 u        -> new String[]{"", readerVar + ".readU64()"};
            case AlgebraicType.I64 i        -> new String[]{"", readerVar + ".readI64()"};
            case AlgebraicType.U128 u       -> new String[]{"", readerVar + ".readU128()"};
            case AlgebraicType.I128 i       -> new String[]{"", readerVar + ".readI128()"};
            case AlgebraicType.U256 u       -> new String[]{"", readerVar + ".readU256()"};
            case AlgebraicType.I256 i       -> new String[]{"", readerVar + ".readI256()"};
            case AlgebraicType.F32 f        -> new String[]{"", readerVar + ".readF32()"};
            case AlgebraicType.F64 f        -> new String[]{"", readerVar + ".readF64()"};
            case AlgebraicType.StringType s -> new String[]{"", readerVar + ".readString()"};
            case AlgebraicType.RefType r    -> {
                String cls = indexToName.getOrDefault(r.ref(), "Object");
                yield new String[]{"", cls + ".fromBsatn(" + readerVar + ")"};
            }
            case AlgebraicType.ArrayType a  -> {
                String lenVar = "_len_" + varName;
                String idxVar = "_i_" + varName;
                String listVar = "_list_" + varName;
                String elemBoxed = boxedJavaType(a.elemTy(), indexToName);
                String[] elemRead = readExpr(a.elemTy(), indexToName, readerVar, varName + "_elem");
                String elemReadLine = elemRead[0].isEmpty()
                        ? listVar + ".add(" + elemRead[1] + ");"
                        : elemRead[0] + "\n        " + listVar + ".add(" + elemRead[1] + ");";
                String preamble = "int " + lenVar + " = " + readerVar + ".readArrayLength();\n" +
                        "        java.util.List<" + elemBoxed + "> " + listVar +
                        " = new java.util.ArrayList<>(" + lenVar + ");\n" +
                        "        for (int " + idxVar + " = 0; " + idxVar + " < " + lenVar + "; " + idxVar + "++) {\n" +
                        "            " + elemReadLine + "\n" +
                        "        }";
                yield new String[]{preamble, listVar};
            }
            default -> new String[]{"", "null /* unsupported: " + type.getClass().getSimpleName() + " */"};
        };
    }

    /**
     * Returns one or more statements (as a single string) that write {@code fieldAccess}
     * of the given type using a BsatnWriter named {@code writerVar}.
     */
    public static String writeStmt(AlgebraicType type, Map<Integer, String> indexToName,
                                   String writerVar, String fieldAccess) {
        return switch (type) {
            case AlgebraicType.Bool b       -> writerVar + ".writeBool(" + fieldAccess + ");";
            case AlgebraicType.U8 u         -> writerVar + ".writeU8(" + fieldAccess + ");";
            case AlgebraicType.I8 i         -> writerVar + ".writeI8(" + fieldAccess + ");";
            case AlgebraicType.U16 u        -> writerVar + ".writeU16(" + fieldAccess + ");";
            case AlgebraicType.I16 i        -> writerVar + ".writeI16(" + fieldAccess + ");";
            case AlgebraicType.U32 u        -> writerVar + ".writeU32(" + fieldAccess + ");";
            case AlgebraicType.I32 i        -> writerVar + ".writeI32(" + fieldAccess + ");";
            case AlgebraicType.U64 u        -> writerVar + ".writeU64(" + fieldAccess + ");";
            case AlgebraicType.I64 i        -> writerVar + ".writeI64(" + fieldAccess + ");";
            case AlgebraicType.U128 u       -> writerVar + ".writeU128(" + fieldAccess + ");";
            case AlgebraicType.I128 i       -> writerVar + ".writeI128(" + fieldAccess + ");";
            case AlgebraicType.U256 u       -> writerVar + ".writeU256(" + fieldAccess + ");";
            case AlgebraicType.I256 i       -> writerVar + ".writeI256(" + fieldAccess + ");";
            case AlgebraicType.F32 f        -> writerVar + ".writeF32(" + fieldAccess + ");";
            case AlgebraicType.F64 f        -> writerVar + ".writeF64(" + fieldAccess + ");";
            case AlgebraicType.StringType s -> writerVar + ".writeString(" + fieldAccess + ");";
            case AlgebraicType.RefType r    ->
                "{ java.nio.ByteBuffer _fb = " + fieldAccess + ".toBsatn();" +
                        " while (_fb.hasRemaining()) " + writerVar + ".writeByte(_fb.get()); }";
            case AlgebraicType.ArrayType a  -> {
                String elemBoxed = boxedJavaType(a.elemTy(), indexToName);
                String elemVar = "_e_" + fieldAccess.replaceAll("[^A-Za-z0-9]", "_");
                String elemWrite = writeStmt(a.elemTy(), indexToName, writerVar, elemVar);
                yield writerVar + ".writeArrayLength(" + fieldAccess + ".size());\n" +
                        "        for (" + elemBoxed + " " + elemVar + " : " + fieldAccess + ") {\n" +
                        "            " + elemWrite + "\n" +
                        "        }";
            }
            default ->
                "/* TODO: write " + type.getClass().getSimpleName() + " field " + fieldAccess + " */";
        };
    }
}
