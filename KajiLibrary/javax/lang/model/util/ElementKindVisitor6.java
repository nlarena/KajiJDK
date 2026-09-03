package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 * KajiLibrary's javax.lang.model.util.ElementKindVisitor6 — el visitante que reparte por
 * {@link ElementKind} y no solo por interfaz.
 *
 * <h2>Que problema resuelve</h2>
 *
 * <p>Las interfaces del modelo son mas gruesas que las declaraciones del lenguaje. Una clase, un enum,
 * una interfaz, un registro y un tipo de anotacion son **los cinco** un `TypeElement`, asi que a
 * {@link SimpleElementVisitor6#visitType} le llegan todos juntos y el que quiera tratarlos distinto
 * tiene que escribir el `if` sobre `getKind()` a mano. Lo mismo con las variables — un campo, un
 * parametro y una constante de enum son todos `VariableElement` — y con los ejecutables, donde un
 * metodo, un constructor y un inicializador comparten `ExecutableElement`.
 *
 * <p>Esta clase escribe ese reparto una sola vez. `visitType` mira el kind y llama a
 * `visitTypeAsClass`, `visitTypeAsEnum` y demas; `visitVariable` y `visitExecutable` hacen lo mismo con
 * los suyos. Los `visitXxxAsYyy` caen a su vez en `defaultAction`, asi que sigue siendo un
 * {@link SimpleElementVisitor6} — se puede redefinir el embudo, o un `visitXxxAsYyy` puntual, o los dos.
 *
 * <h2>El `default` que tira `AssertionError`</h2>
 *
 * <p>Cada reparto termina en un caso que tira. No es un `visitUnknown` disfrazado: significa que llego
 * un `TypeElement` cuyo kind **no es ninguno de los cinco que declaran un tipo**, y eso no es una
 * construccion nueva del lenguaje sino un modelo roto. `visitUnknown` es para lo que el lenguaje agrego
 * despues; `AssertionError` es para lo que nunca pudo ser.
 *
 * <h2>Los tres kinds que este visitante no puede haber previsto</h2>
 *
 * <p>`RESOURCE_VARIABLE` (Java 7), `RECORD` y `BINDING_VARIABLE` (Java 14+) tienen su `visitXxxAsYyy`
 * declarado — hace falta, porque el reparto tiene que poder nombrarlos — pero su cuerpo cae en
 * `visitUnknown` en vez de en `defaultAction`. Es la misma regla de toda la familia: un kind posterior a
 * la version del visitante no se contesta en silencio. Las clases de 7 y de 14 los van pasando al
 * embudo a medida que el lenguaje los incorpora.
 *
 * <p>Notar que el reparto **si** los enumera desde la version 6: si no lo hiciera, una variable de
 * recurso caeria en el `AssertionError` del `default`, que dice algo distinto y equivocado — "modelo
 * roto" en vez de "esto es mas nuevo que yo".
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ElementKindVisitor6<R, P> extends SimpleElementVisitor6<R, P> {

    @Deprecated(since = "9")
    protected ElementKindVisitor6() {
        super(null);
    }

    @Deprecated(since = "9")
    protected ElementKindVisitor6(R defaultValue) {
        super(defaultValue);
    }

    // El reparto va con `if` encadenados y no con `switch`: nuestro generador de bytecode todavia no
    // baja un `switch` cuyo selector no es `int` (COMPILER_FINDINGS #401). Es la misma semantica, porque
    // en el original ninguna rama cae en la siguiente — todas devuelven.

    public R visitPackage(PackageElement e, P p) {
        assert e.getKind() == ElementKind.PACKAGE : "Bad kind on PackageElement";
        return this.defaultAction(e, p);
    }

    public R visitType(TypeElement e, P p) {
        ElementKind k = e.getKind();
        if (k == ElementKind.ANNOTATION_TYPE) {
            return this.visitTypeAsAnnotationType(e, p);
        }
        if (k == ElementKind.CLASS) {
            return this.visitTypeAsClass(e, p);
        }
        if (k == ElementKind.ENUM) {
            return this.visitTypeAsEnum(e, p);
        }
        if (k == ElementKind.INTERFACE) {
            return this.visitTypeAsInterface(e, p);
        }
        if (k == ElementKind.RECORD) {
            return this.visitTypeAsRecord(e, p);
        }
        throw new AssertionError("Bad kind " + k + " for TypeElement" + e);
    }

    public R visitTypeAsAnnotationType(TypeElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitTypeAsClass(TypeElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitTypeAsEnum(TypeElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitTypeAsInterface(TypeElement e, P p) {
        return this.defaultAction(e, p);
    }

    /** Los registros son de Java 14: ver el encabezado. */
    public R visitTypeAsRecord(TypeElement e, P p) {
        return this.visitUnknown(e, p);
    }

    public R visitVariable(VariableElement e, P p) {
        ElementKind k = e.getKind();
        if (k == ElementKind.ENUM_CONSTANT) {
            return this.visitVariableAsEnumConstant(e, p);
        }
        if (k == ElementKind.EXCEPTION_PARAMETER) {
            return this.visitVariableAsExceptionParameter(e, p);
        }
        if (k == ElementKind.FIELD) {
            return this.visitVariableAsField(e, p);
        }
        if (k == ElementKind.LOCAL_VARIABLE) {
            return this.visitVariableAsLocalVariable(e, p);
        }
        if (k == ElementKind.PARAMETER) {
            return this.visitVariableAsParameter(e, p);
        }
        if (k == ElementKind.RESOURCE_VARIABLE) {
            return this.visitVariableAsResourceVariable(e, p);
        }
        if (k == ElementKind.BINDING_VARIABLE) {
            return this.visitVariableAsBindingVariable(e, p);
        }
        throw new AssertionError("Bad kind " + k + " for VariableElement" + e);
    }

    public R visitVariableAsEnumConstant(VariableElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitVariableAsExceptionParameter(VariableElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitVariableAsField(VariableElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitVariableAsLocalVariable(VariableElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitVariableAsParameter(VariableElement e, P p) {
        return this.defaultAction(e, p);
    }

    /** Las variables de recurso son de Java 7: ver el encabezado. */
    public R visitVariableAsResourceVariable(VariableElement e, P p) {
        return this.visitUnknown(e, p);
    }

    /** Las variables de vinculo, las de `instanceof` con patron, son de Java 16: ver el encabezado. */
    public R visitVariableAsBindingVariable(VariableElement e, P p) {
        return this.visitUnknown(e, p);
    }

    public R visitExecutable(ExecutableElement e, P p) {
        ElementKind k = e.getKind();
        if (k == ElementKind.CONSTRUCTOR) {
            return this.visitExecutableAsConstructor(e, p);
        }
        if (k == ElementKind.INSTANCE_INIT) {
            return this.visitExecutableAsInstanceInit(e, p);
        }
        if (k == ElementKind.METHOD) {
            return this.visitExecutableAsMethod(e, p);
        }
        if (k == ElementKind.STATIC_INIT) {
            return this.visitExecutableAsStaticInit(e, p);
        }
        throw new AssertionError("Bad kind " + k + " for ExecutableElement" + e);
    }

    public R visitExecutableAsConstructor(ExecutableElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitExecutableAsInstanceInit(ExecutableElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitExecutableAsMethod(ExecutableElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitExecutableAsStaticInit(ExecutableElement e, P p) {
        return this.defaultAction(e, p);
    }

    // Un parametro de tipo tiene un solo kind, asi que no hay nada que repartir: es el unico `visitXxx`
    // de esta clase que no se abre en `visitXxxAsYyy`.
    public R visitTypeParameter(TypeParameterElement e, P p) {
        assert e.getKind() == ElementKind.TYPE_PARAMETER : "Bad kind on TypeParameterElement";
        return this.defaultAction(e, p);
    }
}
