package javax.lang.model.util;

import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaFileObject;

/**
 * KajiLibrary's javax.lang.model.util.Elements — the questions about elements only the compiler can
 * answer.
 *
 * <p>The counterpart of {@link Types}: where that one reasons about types, this one reasons about
 * **declarations** — looking up a class by its name, knowing whether a method overrides another,
 * reading an element's documentation comment.
 *
 * <p>It is a **pure declaration**: the compiler implements it and hands it over through {@link
 * javax.annotation.processing.ProcessingEnvironment#getElementUtils()}. This library ships no
 * implementation, because what is needed is `javac`'s symbol table, which lives in `src/javac/`.
 *
 * <h2>About the default bodies</h2>
 *
 * <p>More than half the methods are `default`, and their bodies **are an observable part of the
 * API**, not a detail. Two rules were followed here, and it is as well that they be said:
 *
 * <ul>
 * <li>What can be **deduced from the abstract methods** is deduced. `getAllPackageElements` is
 *     built with `getPackageElement`, and `getOutermostTypeElement` climbs the enclosing elements.
 *     That invents nothing: it is the same answer whoever wrote it by hand would reach.</li>
 * <li>What needs support the model may not have --modules, record components, the source file--
 *     returns the answer the contract defines for "no support": `null`, an empty set, or `false`.
 *     **It does not throw.** An implementation that does support it overrides.</li>
 * </ul>
 */
public interface Elements {

    /**
     * Where a declaration came from.
     *
     * <p>The distinction has consequences for a processor: a **synthetic** member --an enum's
     * `values()`, a bridge-- is not in the source, so reporting an error on it points nobody at
     * anything.
     */
    enum Origin {
        /** It is written in the source or in the `.class`. */
        EXPLICIT,
        /** The specification requires it to exist, although nobody wrote it (`Enum.values()`). */
        MANDATED,
        /** The compiler made it and it is not in the specification (a bridge). */
        SYNTHETIC;

        /**
         * Whether this declaration is visible in the source.
         *
         * <p>`EXPLICIT` and `MANDATED` are --both are part of the program the language defines--;
         * `SYNTHETIC` is not.
         */
        public boolean isDeclared() {
            return this != Origin.SYNTHETIC;
        }
    }

    /** The form of the documentation comment: `/** ... *&#47;` or a run of `///`. */
    enum DocCommentKind {
        /** Several lines starting with `///`. */
        END_OF_LINE,
        /** The classic delimited one. */
        TRADITIONAL
    }

    /** The package with that canonical name, or `null` if there is none. */
    PackageElement getPackageElement(CharSequence name);

    /**
     * The package with that name within that module, or `null`.
     *
     * <p>By default `null`: a model without modules has nowhere to look. It does not delegate to
     * the one-argument version on purpose — that would be answering for a module other than the one
     * asked for.
     */
    default PackageElement getPackageElement(ModuleElement module, CharSequence name) {
        return null;
    }

    /**
     * All the packages with that name, in all the modules.
     *
     * <p>It is deduced: without modules there is at most one, the one {@link #getPackageElement}
     * returns.
     */
    default Set<? extends PackageElement> getAllPackageElements(CharSequence name) {
        // The set is always built and returned empty if there was nothing, instead of leaving early
        // with `Collections.emptySet()`: the frozen javac that builds this library does not infer
        // the type argument when the target is a supertype **with a wildcard** (`Set<? extends
        // X>`), and reports "incompatible return type". With the local variable written out, it
        // resolves. (The source-built javac infers it; checked 2026-09-18.)
        Set<PackageElement> out = new LinkedHashSet<PackageElement>();
        PackageElement p = this.getPackageElement(name);
        if (p != null) {
            out.add(p);
        }
        return out;
    }

    /** The type with that canonical name, or `null`. */
    TypeElement getTypeElement(CharSequence name);

    /**
     * The type with that name within that module, or `null`. By default `null`, like the package.
     */
    default TypeElement getTypeElement(ModuleElement module, CharSequence name) {
        return null;
    }

    /** All the types with that name. Deduced from {@link #getTypeElement}. */
    default Set<? extends TypeElement> getAllTypeElements(CharSequence name) {
        Set<TypeElement> out = new LinkedHashSet<TypeElement>();
        TypeElement t = this.getTypeElement(name);
        if (t != null) {
            out.add(t);
        }
        return out;
    }

    /** The module with that name, or `null` if there are no modules in the model. */
    default ModuleElement getModuleElement(CharSequence name) {
        return null;
    }

    /** All the modules, or the empty set if the model has none. */
    default Set<? extends ModuleElement> getAllModuleElements() {
        return new LinkedHashSet<ModuleElement>();
    }

    /**
     * The values of that annotation, **with the ones not written filled in with their default**.
     *
     * <p>It is the difference from {@link AnnotationMirror#getElementValues()}, which returns only
     * what was written. A processor almost always wants this one: `@Retention` with no explicit
     * `value` still has a policy.
     */
    Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(
            AnnotationMirror a);

    /** The documentation comment, without delimiters, or `null` if it has none. */
    String getDocComment(Element e);

    /**
     * The form of the comment, or `null` if it has none.
     *
     * <p>By default: if there is a comment it is {@link DocCommentKind#TRADITIONAL}. It is right
     * for a model that does not tell the two forms apart — the traditional one is the one that
     * always existed.
     */
    default DocCommentKind getDocCommentKind(Element e) {
        return this.getDocComment(e) == null ? null : DocCommentKind.TRADITIONAL;
    }

    /** Whether it is marked deprecated. */
    boolean isDeprecated(Element e);

    /** Where that element came from. By default {@link Origin#EXPLICIT}. */
    default Origin getOrigin(Element e) {
        return Origin.EXPLICIT;
    }

    /** Where that annotation came from. By default {@link Origin#EXPLICIT}. */
    default Origin getOrigin(AnnotatedConstruct c, AnnotationMirror a) {
        return Origin.EXPLICIT;
    }

    /** Where that module directive came from. By default {@link Origin#EXPLICIT}. */
    default Origin getOrigin(ModuleElement m, ModuleElement.Directive directive) {
        return Origin.EXPLICIT;
    }

    /**
     * Whether it is a **bridge**, the synthetic method the compiler adds so that a covariant return
     * works with erasure.
     *
     * <p>By default `false`: a model that makes no bridges has none.
     */
    default boolean isBridge(ExecutableElement e) {
        return false;
    }

    /**
     * The binary name of that type.
     *
     * <p>It is not the canonical one: a nested type is `Outer$Inner` here and `Outer.Inner` there.
     * It is the one to use for naming the `.class`.
     */
    Name getBinaryName(TypeElement type);

    /** The package that contains that element. */
    PackageElement getPackageOf(Element e);

    /** The module that contains it, or `null` if the model has no modules. */
    default ModuleElement getModuleOf(Element e) {
        return null;
    }

    /**
     * All the members of that type, **inherited ones included**.
     *
     * <p>It is the difference from {@link Element#getEnclosedElements()}, which gives only the
     * declared ones. Inheritance has to be resolved by the compiler, and that is why it is here and
     * not in the element.
     */
    List<? extends Element> getAllMembers(TypeElement type);

    /**
     * The top-level type that contains it, or `null` if it is not inside any.
     *
     * <p>It is deduced by climbing the enclosing elements up to the last one that is a type.
     */
    default TypeElement getOutermostTypeElement(Element e) {
        TypeElement outermost = null;
        Element cur = e;
        while (cur != null) {
            if (cur instanceof TypeElement) {
                outermost = (TypeElement) cur;
            }
            cur = cur.getEnclosingElement();
        }
        return outermost;
    }

    /** All the annotations of that element, inherited ones included. */
    List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e);

    /**
     * Whether `hider` hides `hidden`.
     *
     * <p>Hiding is not overriding: a field or a static method of a subclass **hides** the
     * superclass's, and which one is used is decided by the static type. Overriding belongs to
     * instance methods, and the dynamic type decides it. That is why they are two different
     * methods.
     */
    boolean hides(Element hider, Element hidden);

    /** Whether `overrider`, seen as a member of `type`, overrides `overridden`. */
    boolean overrides(ExecutableElement overrider, ExecutableElement overridden, TypeElement type);

    /**
     * That constant value written as a Java expression.
     *
     * <p>It serves for generating code: a `char` comes out as `'a'` and a `String` with its escapes
     * in place, so that the text can be pasted into a source file and compile.
     */
    String getConstantExpression(Object value);

    /** Writes a representation of those elements, for debugging. */
    void printElements(Writer w, Element... elements);

    /**
     * A {@link Name} with that content.
     *
     * <p>It exists because the model's `Name`s are compared by **identity**, not with `equals`: the
     * compiler interns them. A `Name` made by other means would not match the model's.
     */
    Name getName(CharSequence cs);

    /** Whether that type is a functional interface (§9.8). */
    boolean isFunctionalInterface(TypeElement type);

    /** Whether it is an automatic module. By default `false`. */
    default boolean isAutomaticModule(ModuleElement module) {
        return false;
    }

    /**
     * The class body of that enum constant, or `null` if it has none.
     *
     * <p>An enum constant may bring its own body (`RED { ... }`), and in that case it is an
     * anonymous subclass. By default `null`: none has one until the model says so.
     */
    default TypeElement getEnumConstantBody(VariableElement enumConstant) {
        return null;
    }

    /** The record component that accessor returns, or `null` if it is not an accessor. */
    default RecordComponentElement recordComponentFor(ExecutableElement accessor) {
        return null;
    }

    /** Whether it is a record's canonical constructor. By default `false`. */
    default boolean isCanonicalConstructor(ExecutableElement e) {
        return false;
    }

    /** Whether it is a record's compact constructor. By default `false`. */
    default boolean isCompactConstructor(ExecutableElement e) {
        return false;
    }

    /** The file that element came from, or `null` if not known. */
    default JavaFileObject getFileObjectOf(Element e) {
        return null;
    }
}
