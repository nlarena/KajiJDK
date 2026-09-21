package jdk.internal.apt;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

// The **reification** of a symbol of the compiler (`javax.lang.model` as the annotation
// processor sees it): an object of the heap that wraps the `SymbolId` of a `Symbol` of the
// symbol table of `javac`. The VM builds it (see `jvm::interpreter::apt`) and its methods
// read the table --alive in the process itself-- through that `sym`.
//
// Layers 2-5: it already `implements TypeElement` for real. The name travels as a `Name`
// (`SymName`, not `String`); the FQN comes from the *binary name* with the `$` of the nesting
// turned into `.`; the enclosing element is reified via `Symbol.owner` (with the same identity
// cache as `element_for`). `getKind()`/`getEnclosedElements()` are **not** natives of the
// bridge but **intrinsics** over `Exec`: the first one returns a constant of an enum
// (it needs to run the `<clinit>` of `ElementKind`), the second one builds a `List` and
// re-enters the interpreter for each member (`ArrayList.add`) -- things the bridge (with no
// `Exec` view) cannot do. Both are declared `native` here only so that the class
// brings no body: the interpreter intercepts them before they reach the bridge.
public final class SymElement implements TypeElement {
    // The `SymbolId` (stable index into `SymbolTable.symbols`) this element reifies.
    // The VM writes it when building the object; the natives/intrinsics below read it in order
    // to index the table of the compiler.
    int sym;

    // The simple name of the symbol (`Symbol.name`): "Foo" for `class Foo {}`, wrapped in
    // a `Name` (`SymName`).
    public native Name getSimpleName();

    // The qualified name of the type (`Symbol.binary` with `$` -> `.`): "a.b.Outer.Inner".
    public native Name getQualifiedName();

    // The element that encloses it lexically (`Symbol.owner`): the class of a member, the
    // package of a top-level type. `null` if it has no owner. The same identity as
    // `element_for`, so two children of the same owner return the **same** object.
    public native Element getEnclosingElement();

    // The `ElementKind` class of this symbol (CLASS/INTERFACE/ENUM/METHOD/...). An intrinsic.
    public native ElementKind getKind();

    // The elements declared directly by this one (its members), as a `List`.
    // An intrinsic: it builds an `ArrayList` and reifies each member.
    public native List<? extends Element> getEnclosedElements();

    // ---- what `TypeElement` asks for and is not served yet --------------------------------------
    //
    // They were **undeclared**, and that was not a pending layer but a concrete class that was
    // missing inherited abstract methods: in real Java it does not compile, and here it compiled
    // because the check was skipped when the supertype came from the classpath (finding #284).
    //
    // They go `native` like the ones above, which is the convention of this file and of the
    // library: a site that uses them falls into the `native` with no bridge and receives an
    // `UnsatisfiedLinkError`, which tells the truth --"this is not there"--. Returning `null` or an
    // empty list would compile all the same and would lie, which is precisely what the rule of the
    // house forbids.

    /**
     * The type this element declares. It needs to reify a `TypeMirror`, which does not exist yet.
     */
    public native TypeMirror asType();

    public native NestingKind getNestingKind();

    public native TypeMirror getSuperclass();

    public native List<? extends TypeMirror> getInterfaces();

    public native List<? extends TypeParameterElement> getTypeParameters();

    public native Set<Modifier> getModifiers();

    public native List<? extends AnnotationMirror> getAnnotationMirrors();

    public native <A extends Annotation> A getAnnotation(Class<A> annotationType);

    public native <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType);

    public native <R, P> R accept(ElementVisitor<R, P> v, P p);
}
