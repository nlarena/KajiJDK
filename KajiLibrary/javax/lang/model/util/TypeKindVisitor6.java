package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;

/**
 * KajiLibrary's javax.lang.model.util.TypeKindVisitor6 — el visitante que reparte por {@link TypeKind} y
 * no solo por interfaz.
 *
 * <p>Misma idea que {@link ElementKindVisitor6}, del lado de los tipos, y con dos repartos en vez de
 * tres. Son los dos lugares donde una interfaz del modelo tapa varias formas distintas:
 *
 * <ul>
 * <li>{@link PrimitiveType} tapa los ocho primitivos. `visitPrimitive` los abre en
 *     `visitPrimitiveAsInt`, `visitPrimitiveAsBoolean` y demas — que es lo que hace falta casi siempre,
 *     porque un visitante que trate `int` y `double` igual es raro.</li>
 * <li>{@link NoType} tapa los pseudotipos: `void`, el de un paquete, el de un modulo y `NONE`. Cuatro
 *     cosas que no son tipos, por cuatro razones distintas, bajo una sola interfaz.</li>
 * </ul>
 *
 * <p>El resto de las formas de tipo — array, declarado, comodin — tienen su interfaz propia y no
 * necesitan reparto, asi que se heredan tal cual de {@link SimpleTypeVisitor6}.
 *
 * <p>El `AssertionError` del final de cada reparto significa lo mismo que en `ElementKindVisitor6`: un
 * `PrimitiveType` cuyo kind no es primitivo es un modelo roto, no una construccion nueva del lenguaje.
 *
 * <p>`visitNoTypeAsModule` es el unico caso que cae en `visitUnknown`: el pseudotipo `MODULE` es de Java
 * 9 y este visitante es de 6. {@link TypeKindVisitor9} lo pasa al embudo.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TypeKindVisitor6<R, P> extends SimpleTypeVisitor6<R, P> {

    @Deprecated(since = "9")
    protected TypeKindVisitor6() {
        super(null);
    }

    @Deprecated(since = "9")
    protected TypeKindVisitor6(R defaultValue) {
        super(defaultValue);
    }

    // `if` encadenados y no `switch`, por lo mismo que en ElementKindVisitor6 (COMPILER_FINDINGS #401).

    public R visitPrimitive(PrimitiveType t, P p) {
        TypeKind k = t.getKind();
        if (k == TypeKind.BOOLEAN) {
            return this.visitPrimitiveAsBoolean(t, p);
        }
        if (k == TypeKind.BYTE) {
            return this.visitPrimitiveAsByte(t, p);
        }
        if (k == TypeKind.SHORT) {
            return this.visitPrimitiveAsShort(t, p);
        }
        if (k == TypeKind.INT) {
            return this.visitPrimitiveAsInt(t, p);
        }
        if (k == TypeKind.LONG) {
            return this.visitPrimitiveAsLong(t, p);
        }
        if (k == TypeKind.CHAR) {
            return this.visitPrimitiveAsChar(t, p);
        }
        if (k == TypeKind.FLOAT) {
            return this.visitPrimitiveAsFloat(t, p);
        }
        if (k == TypeKind.DOUBLE) {
            return this.visitPrimitiveAsDouble(t, p);
        }
        throw new AssertionError("Bad kind " + k + " for PrimitiveType" + t);
    }

    public R visitPrimitiveAsBoolean(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsByte(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsShort(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsInt(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsLong(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsChar(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsFloat(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsDouble(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitNoType(NoType t, P p) {
        TypeKind k = t.getKind();
        if (k == TypeKind.VOID) {
            return this.visitNoTypeAsVoid(t, p);
        }
        if (k == TypeKind.PACKAGE) {
            return this.visitNoTypeAsPackage(t, p);
        }
        if (k == TypeKind.MODULE) {
            return this.visitNoTypeAsModule(t, p);
        }
        if (k == TypeKind.NONE) {
            return this.visitNoTypeAsNone(t, p);
        }
        throw new AssertionError("Bad kind " + k + " for NoType" + t);
    }

    public R visitNoTypeAsVoid(NoType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitNoTypeAsPackage(NoType t, P p) {
        return this.defaultAction(t, p);
    }

    /** El pseudotipo de un modulo es de Java 9: ver el encabezado. */
    public R visitNoTypeAsModule(NoType t, P p) {
        return this.visitUnknown(t, p);
    }

    public R visitNoTypeAsNone(NoType t, P p) {
        return this.defaultAction(t, p);
    }
}
