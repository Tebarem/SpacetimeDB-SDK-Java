package eu.jlavocat.spacetimedb.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AlgebraicType.Bool.class,       name = "Bool"),
    @JsonSubTypes.Type(value = AlgebraicType.U8.class,         name = "U8"),
    @JsonSubTypes.Type(value = AlgebraicType.I8.class,         name = "I8"),
    @JsonSubTypes.Type(value = AlgebraicType.U16.class,        name = "U16"),
    @JsonSubTypes.Type(value = AlgebraicType.I16.class,        name = "I16"),
    @JsonSubTypes.Type(value = AlgebraicType.U32.class,        name = "U32"),
    @JsonSubTypes.Type(value = AlgebraicType.I32.class,        name = "I32"),
    @JsonSubTypes.Type(value = AlgebraicType.U64.class,        name = "U64"),
    @JsonSubTypes.Type(value = AlgebraicType.I64.class,        name = "I64"),
    @JsonSubTypes.Type(value = AlgebraicType.U128.class,       name = "U128"),
    @JsonSubTypes.Type(value = AlgebraicType.I128.class,       name = "I128"),
    @JsonSubTypes.Type(value = AlgebraicType.U256.class,       name = "U256"),
    @JsonSubTypes.Type(value = AlgebraicType.I256.class,       name = "I256"),
    @JsonSubTypes.Type(value = AlgebraicType.F32.class,        name = "F32"),
    @JsonSubTypes.Type(value = AlgebraicType.F64.class,        name = "F64"),
    @JsonSubTypes.Type(value = AlgebraicType.StringType.class, name = "String"),
    @JsonSubTypes.Type(value = AlgebraicType.ProductType.class, name = "Product"),
    @JsonSubTypes.Type(value = AlgebraicType.SumType.class,    name = "Sum"),
    @JsonSubTypes.Type(value = AlgebraicType.RefType.class,    name = "Ref"),
    @JsonSubTypes.Type(value = AlgebraicType.ArrayType.class,  name = "Array"),
    @JsonSubTypes.Type(value = AlgebraicType.MapType.class,    name = "Map"),
})
public sealed interface AlgebraicType permits
        AlgebraicType.Bool, AlgebraicType.U8, AlgebraicType.I8,
        AlgebraicType.U16, AlgebraicType.I16, AlgebraicType.U32,
        AlgebraicType.I32, AlgebraicType.U64, AlgebraicType.I64,
        AlgebraicType.U128, AlgebraicType.I128, AlgebraicType.U256,
        AlgebraicType.I256, AlgebraicType.F32, AlgebraicType.F64,
        AlgebraicType.StringType, AlgebraicType.ProductType, AlgebraicType.SumType,
        AlgebraicType.RefType, AlgebraicType.ArrayType, AlgebraicType.MapType {

    record Bool()       implements AlgebraicType {}
    record U8()         implements AlgebraicType {}
    record I8()         implements AlgebraicType {}
    record U16()        implements AlgebraicType {}
    record I16()        implements AlgebraicType {}
    record U32()        implements AlgebraicType {}
    record I32()        implements AlgebraicType {}
    record U64()        implements AlgebraicType {}
    record I64()        implements AlgebraicType {}
    record U128()       implements AlgebraicType {}
    record I128()       implements AlgebraicType {}
    record U256()       implements AlgebraicType {}
    record I256()       implements AlgebraicType {}
    record F32()        implements AlgebraicType {}
    record F64()        implements AlgebraicType {}
    record StringType() implements AlgebraicType {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    record ProductType(@JsonProperty("elements") List<ProductElement> elements) implements AlgebraicType {}
    record SumType(@JsonProperty("variants") List<SumVariant> variants)         implements AlgebraicType {}

    record ArrayType(@JsonProperty("elem_ty") AlgebraicType elemTy) implements AlgebraicType {}
    record MapType(
            @JsonProperty("key_ty") AlgebraicType keyTy,
            @JsonProperty("ty")     AlgebraicType valueTy
    ) implements AlgebraicType {}

    /** Deserializes from a plain integer, e.g. {"Ref": 3}. */
    record RefType(int ref) implements AlgebraicType {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public RefType(int ref) {
            this.ref = ref;
        }
    }
}
