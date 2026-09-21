package javax.lang.model.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * KajiLibrary's javax.lang.model.util.ElementFilter — keeping the members of a class.
 *
 * <p>{@link javax.lang.model.element.Element#getEnclosedElements()} returns
 * `List&lt;? extends Element&gt;`: the fields, the methods, the constructors and the nested types
 * mixed and typed with the common supertype. Almost no processor wants that list. It wants "the
 * fields", and wants them as `VariableElement` to be able to ask them
 * {@link VariableElement#getConstantValue()} without casting by hand in each iteration.
 *
 * <p>That is all this class does, and that is why there are twenty methods that look alike: each
 * one filters by a set of {@link ElementKind}s and casts to the subtype that set guarantees. The
 * filter goes by **kind and not by `instanceof`** because the kind is the right question: an enum
 * constant and a field are both `VariableElement`s, and `fieldsIn` wants both, but a parameter is
 * also a `VariableElement` and is not a field. `instanceof` cannot tell them apart; the kind can.
 *
 * <p>Each filter comes in two forms, and the difference matters: the one taking an `Iterable`
 * returns a `List` --the normal one, over `getEnclosedElements()`--; the one taking a `Set` returns
 * a `Set` and **keeps the iteration order** of the input set, so that filtering a `LinkedHashSet`
 * does not scramble it.
 */
public class ElementFilter {

    // Private constructor: they are all static methods and an instance would mean nothing.
    private ElementFilter() {
    }

    private static final Set<ElementKind> CONSTRUCTOR_KIND =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.CONSTRUCTOR));

    // An enum constant is a field for the language, so `fieldsIn` includes it.
    private static final Set<ElementKind> FIELD_KINDS =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.FIELD, ElementKind.ENUM_CONSTANT));

    private static final Set<ElementKind> METHOD_KIND =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.METHOD));

    private static final Set<ElementKind> PACKAGE_KIND =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.PACKAGE));

    private static final Set<ElementKind> MODULE_KIND =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.MODULE));

    // The five kinds that declare a type. An `@interface` is ANNOTATION_TYPE and not INTERFACE, and
    // a record is RECORD and not CLASS: if the five were not there, `typesIn` would skip
    // declarations that are types.
    private static final Set<ElementKind> TYPE_KINDS =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.CLASS, ElementKind.ENUM,
                    ElementKind.INTERFACE, ElementKind.RECORD, ElementKind.ANNOTATION_TYPE));

    private static final Set<ElementKind> RECORD_COMPONENT_KIND =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.RECORD_COMPONENT));

    /** The fields and the enum constants. */
    public static List<VariableElement> fieldsIn(Iterable<? extends Element> elements) {
        return listFilter(elements, FIELD_KINDS, VariableElement.class);
    }

    /** The fields and the enum constants, keeping the set's order. */
    public static Set<VariableElement> fieldsIn(Set<? extends Element> elements) {
        return setFilter(elements, FIELD_KINDS, VariableElement.class);
    }

    /** The record components. */
    public static List<RecordComponentElement> recordComponentsIn(
            Iterable<? extends Element> elements) {
        return listFilter(elements, RECORD_COMPONENT_KIND, RecordComponentElement.class);
    }

    /** The record components, keeping the set's order. */
    public static Set<RecordComponentElement> recordComponentsIn(Set<? extends Element> elements) {
        return setFilter(elements, RECORD_COMPONENT_KIND, RecordComponentElement.class);
    }

    /** The constructors. It does not include the initializers, which have their own kind. */
    public static List<ExecutableElement> constructorsIn(Iterable<? extends Element> elements) {
        return listFilter(elements, CONSTRUCTOR_KIND, ExecutableElement.class);
    }

    /** The constructors, keeping the set's order. */
    public static Set<ExecutableElement> constructorsIn(Set<? extends Element> elements) {
        return setFilter(elements, CONSTRUCTOR_KIND, ExecutableElement.class);
    }

    /**
     * The methods. Neither constructors nor initializers, although all three are
     * `ExecutableElement`s.
     */
    public static List<ExecutableElement> methodsIn(Iterable<? extends Element> elements) {
        return listFilter(elements, METHOD_KIND, ExecutableElement.class);
    }

    /** The methods, keeping the set's order. */
    public static Set<ExecutableElement> methodsIn(Set<? extends Element> elements) {
        return setFilter(elements, METHOD_KIND, ExecutableElement.class);
    }

    /** The classes, enums, interfaces, records and annotation types. */
    public static List<TypeElement> typesIn(Iterable<? extends Element> elements) {
        return listFilter(elements, TYPE_KINDS, TypeElement.class);
    }

    /** The types, keeping the set's order. */
    public static Set<TypeElement> typesIn(Set<? extends Element> elements) {
        return setFilter(elements, TYPE_KINDS, TypeElement.class);
    }

    /** The packages. */
    public static List<PackageElement> packagesIn(Iterable<? extends Element> elements) {
        return listFilter(elements, PACKAGE_KIND, PackageElement.class);
    }

    /** The packages, keeping the set's order. */
    public static Set<PackageElement> packagesIn(Set<? extends Element> elements) {
        return setFilter(elements, PACKAGE_KIND, PackageElement.class);
    }

    /** The modules. */
    public static List<ModuleElement> modulesIn(Iterable<? extends Element> elements) {
        return listFilter(elements, MODULE_KIND, ModuleElement.class);
    }

    /** The modules, keeping the set's order. */
    public static Set<ModuleElement> modulesIn(Set<? extends Element> elements) {
        return setFilter(elements, MODULE_KIND, ModuleElement.class);
    }

    // The cast goes through `Class.cast` and not `(E)`: erasure would make a written cast be
    // checked not here but only where the caller uses the element, and there the ClassCastException
    // would point at the wrong place. `Class.cast` fails in the filter, which is where the error
    // is.
    private static <E extends Element> List<E> listFilter(Iterable<? extends Element> elements,
            Set<ElementKind> targetKinds, Class<E> clazz) {
        List<E> list = new ArrayList<E>();
        for (Element e : elements) {
            if (targetKinds.contains(e.getKind())) {
                list.add(clazz.cast(e));
            }
        }
        return list;
    }

    // `LinkedHashSet` and not `HashSet`: the contract promises to keep the input's iteration order,
    // and that is exactly what a HashSet would lose.
    private static <E extends Element> Set<E> setFilter(Set<? extends Element> elements,
            Set<ElementKind> targetKinds, Class<E> clazz) {
        Set<E> set = new LinkedHashSet<E>();
        for (Element e : elements) {
            if (targetKinds.contains(e.getKind())) {
                set.add(clazz.cast(e));
            }
        }
        return set;
    }

    /** A module's `exports` directives. */
    public static List<ModuleElement.ExportsDirective> exportsIn(
            Iterable<? extends ModuleElement.Directive> directives) {
        return listFilter(directives, ModuleElement.DirectiveKind.EXPORTS,
                ModuleElement.ExportsDirective.class);
    }

    /** The `opens` directives. */
    public static List<ModuleElement.OpensDirective> opensIn(
            Iterable<? extends ModuleElement.Directive> directives) {
        return listFilter(directives, ModuleElement.DirectiveKind.OPENS,
                ModuleElement.OpensDirective.class);
    }

    /** The `provides` directives. */
    public static List<ModuleElement.ProvidesDirective> providesIn(
            Iterable<? extends ModuleElement.Directive> directives) {
        return listFilter(directives, ModuleElement.DirectiveKind.PROVIDES,
                ModuleElement.ProvidesDirective.class);
    }

    /** The `requires` directives. */
    public static List<ModuleElement.RequiresDirective> requiresIn(
            Iterable<? extends ModuleElement.Directive> directives) {
        return listFilter(directives, ModuleElement.DirectiveKind.REQUIRES,
                ModuleElement.RequiresDirective.class);
    }

    /** The `uses` directives. */
    public static List<ModuleElement.UsesDirective> usesIn(
            Iterable<? extends ModuleElement.Directive> directives) {
        return listFilter(directives, ModuleElement.DirectiveKind.USES,
                ModuleElement.UsesDirective.class);
    }

    // The directives are filtered by kind equality and not by a set: each of the five has exactly
    // one kind, and an `==` on an enum says the same without building anything.
    private static <D extends ModuleElement.Directive> List<D> listFilter(
            Iterable<? extends ModuleElement.Directive> directives,
            ModuleElement.DirectiveKind directiveKind, Class<D> clazz) {
        List<D> list = new ArrayList<D>();
        for (ModuleElement.Directive d : directives) {
            if (d.getKind() == directiveKind) {
                list.add(clazz.cast(d));
            }
        }
        return list;
    }
}
