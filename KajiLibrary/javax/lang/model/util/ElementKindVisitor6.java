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
 * KajiLibrary's javax.lang.model.util.ElementKindVisitor6 — the visitor that dispatches by
 * {@link ElementKind} and not only by interface.
 *
 * <h2>What problem it solves</h2>
 *
 * <p>The model's interfaces are coarser than the language's declarations. A class, an enum, an
 * interface, a record and an annotation type are **all five** a `TypeElement`, so {@link
 * SimpleElementVisitor6#visitType} receives them all together and whoever wants to treat them
 * differently has to write the `if` on `getKind()` by hand. The same with variables — a field, a
 * parameter and an enum constant are all `VariableElement`s — and with executables, where a method,
 * a constructor and an initializer share `ExecutableElement`.
 *
 * <p>This class writes that dispatch once. `visitType` looks at the kind and calls
 * `visitTypeAsClass`, `visitTypeAsEnum` and so on; `visitVariable` and `visitExecutable` do the
 * same with theirs. The `visitXxxAsYyy` methods fall in turn into `defaultAction`, so it is still a
 * {@link SimpleElementVisitor6} — one can override the funnel, or a particular `visitXxxAsYyy`, or
 * both.
 *
 * <h2>The `default` that throws `AssertionError`</h2>
 *
 * <p>Each dispatch ends in a case that throws. It is not a disguised `visitUnknown`: it means a
 * `TypeElement` arrived whose kind **is none of the five that declare a type**, and that is not a
 * new language construct but a broken model. `visitUnknown` is for what the language added later;
 * `AssertionError` is for what never could be.
 *
 * <h2>The three kinds this visitor cannot have foreseen</h2>
 *
 * <p>`RESOURCE_VARIABLE` (Java 7), `RECORD` and `BINDING_VARIABLE` (Java 14+) have their
 * `visitXxxAsYyy` declared — it is needed, because the dispatch has to be able to name them — but
 * their body falls into `visitUnknown` instead of `defaultAction`. It is the rule of the whole
 * family: a kind later than the visitor's version is not answered silently. The 7 and 14 classes
 * pass them to the funnel as the language takes them in.
 *
 * <p>Note that the dispatch **does** enumerate them from version 6: if it did not, a resource
 * variable would fall into the `default`'s `AssertionError`, which says something different and
 * wrong — "broken model" instead of "this is newer than me".
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

    // The dispatch goes with chained `if`s and not with a `switch`: the frozen javac that builds
    // this library does not lower a `switch` on an enum read from the class path. The note blamed
    // COMPILER_FINDINGS #401; #401 is closed, and the case that remained was #538, closed too --
    // the source-built javac compiles such a switch, the frozen one does not (checked 2026-09-18).
    // It is the same semantics, because in the original no branch falls into the next — they all
    // return.

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

    /** Records are from Java 14: see the header. */
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

    /** Resource variables are from Java 7: see the header. */
    public R visitVariableAsResourceVariable(VariableElement e, P p) {
        return this.visitUnknown(e, p);
    }

    /** Binding variables, those of pattern `instanceof`, are from Java 16: see the header. */
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

    // A type parameter has a single kind, so there is nothing to dispatch: it is the only
    // `visitXxx` of this class that does not open into `visitXxxAsYyy`.
    public R visitTypeParameter(TypeParameterElement e, P p) {
        assert e.getKind() == ElementKind.TYPE_PARAMETER : "Bad kind on TypeParameterElement";
        return this.defaultAction(e, p);
    }
}
