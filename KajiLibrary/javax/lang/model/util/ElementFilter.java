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
 * KajiLibrary's javax.lang.model.util.ElementFilter — quedarse con los miembros de una clase.
 *
 * <p>{@link javax.lang.model.element.Element#getEnclosedElements()} devuelve
 * `List&lt;? extends Element&gt;`: los campos, los metodos, los constructores y los tipos anidados
 * mezclados y tipados con el supertipo comun. Casi ningun procesador quiere esa lista. Quiere "los
 * campos", y los quiere como `VariableElement` para poder preguntarles
 * {@link VariableElement#getConstantValue()} sin castear a mano en cada iteracion.
 *
 * <p>Eso es todo lo que hace esta clase, y por eso son veinte metodos que se parecen: cada uno filtra
 * por un juego de {@link ElementKind} y castea al subtipo que ese juego garantiza. El filtro va por
 * **kind y no por `instanceof`** porque el kind es la pregunta correcta: una constante de enum y un
 * campo son los dos `VariableElement`, y `fieldsIn` los quiere a los dos, pero un parametro tambien es
 * `VariableElement` y no es un campo. `instanceof` no sabe distinguirlos; el kind si.
 *
 * <p>Cada filtro viene en dos formas, y la diferencia importa: la que toma `Iterable` devuelve
 * `List` --lo normal, sobre `getEnclosedElements()`--; la que toma `Set` devuelve `Set` y **conserva
 * el orden de iteracion** del conjunto de entrada, para que filtrar un `LinkedHashSet` no lo
 * desordene.
 */
public class ElementFilter {

    // Constructor privado: son todos metodos estaticos y una instancia no significaria nada.
    private ElementFilter() {
    }

    private static final Set<ElementKind> CONSTRUCTOR_KIND =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.CONSTRUCTOR));

    // Una constante de enum es un campo para el lenguaje, asi que `fieldsIn` la incluye.
    private static final Set<ElementKind> FIELD_KINDS =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.FIELD, ElementKind.ENUM_CONSTANT));

    private static final Set<ElementKind> METHOD_KIND =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.METHOD));

    private static final Set<ElementKind> PACKAGE_KIND =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.PACKAGE));

    private static final Set<ElementKind> MODULE_KIND =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.MODULE));

    // Los cinco kinds que declaran un tipo. Un `@interface` es ANNOTATION_TYPE y no INTERFACE, y un
    // registro es RECORD y no CLASS: si no estuvieran los cinco, `typesIn` se saltearia declaraciones
    // que son tipos.
    private static final Set<ElementKind> TYPE_KINDS =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.CLASS, ElementKind.ENUM,
                    ElementKind.INTERFACE, ElementKind.RECORD, ElementKind.ANNOTATION_TYPE));

    private static final Set<ElementKind> RECORD_COMPONENT_KIND =
            Collections.unmodifiableSet(EnumSet.of(ElementKind.RECORD_COMPONENT));

    /** Los campos y las constantes de enum. */
    public static List<VariableElement> fieldsIn(Iterable<? extends Element> elements) {
        return listFilter(elements, FIELD_KINDS, VariableElement.class);
    }

    /** Los campos y las constantes de enum, conservando el orden del conjunto. */
    public static Set<VariableElement> fieldsIn(Set<? extends Element> elements) {
        return setFilter(elements, FIELD_KINDS, VariableElement.class);
    }

    /** Los componentes de registro. */
    public static List<RecordComponentElement> recordComponentsIn(
            Iterable<? extends Element> elements) {
        return listFilter(elements, RECORD_COMPONENT_KIND, RecordComponentElement.class);
    }

    /** Los componentes de registro, conservando el orden del conjunto. */
    public static Set<RecordComponentElement> recordComponentsIn(Set<? extends Element> elements) {
        return setFilter(elements, RECORD_COMPONENT_KIND, RecordComponentElement.class);
    }

    /** Los constructores. No incluye los inicializadores, que tienen su propio kind. */
    public static List<ExecutableElement> constructorsIn(Iterable<? extends Element> elements) {
        return listFilter(elements, CONSTRUCTOR_KIND, ExecutableElement.class);
    }

    /** Los constructores, conservando el orden del conjunto. */
    public static Set<ExecutableElement> constructorsIn(Set<? extends Element> elements) {
        return setFilter(elements, CONSTRUCTOR_KIND, ExecutableElement.class);
    }

    /** Los metodos. Ni constructores ni inicializadores, aunque los tres sean `ExecutableElement`. */
    public static List<ExecutableElement> methodsIn(Iterable<? extends Element> elements) {
        return listFilter(elements, METHOD_KIND, ExecutableElement.class);
    }

    /** Los metodos, conservando el orden del conjunto. */
    public static Set<ExecutableElement> methodsIn(Set<? extends Element> elements) {
        return setFilter(elements, METHOD_KIND, ExecutableElement.class);
    }

    /** Las clases, enums, interfaces, registros y tipos de anotacion. */
    public static List<TypeElement> typesIn(Iterable<? extends Element> elements) {
        return listFilter(elements, TYPE_KINDS, TypeElement.class);
    }

    /** Los tipos, conservando el orden del conjunto. */
    public static Set<TypeElement> typesIn(Set<? extends Element> elements) {
        return setFilter(elements, TYPE_KINDS, TypeElement.class);
    }

    /** Los paquetes. */
    public static List<PackageElement> packagesIn(Iterable<? extends Element> elements) {
        return listFilter(elements, PACKAGE_KIND, PackageElement.class);
    }

    /** Los paquetes, conservando el orden del conjunto. */
    public static Set<PackageElement> packagesIn(Set<? extends Element> elements) {
        return setFilter(elements, PACKAGE_KIND, PackageElement.class);
    }

    /** Los modulos. */
    public static List<ModuleElement> modulesIn(Iterable<? extends Element> elements) {
        return listFilter(elements, MODULE_KIND, ModuleElement.class);
    }

    /** Los modulos, conservando el orden del conjunto. */
    public static Set<ModuleElement> modulesIn(Set<? extends Element> elements) {
        return setFilter(elements, MODULE_KIND, ModuleElement.class);
    }

    // El cast va por `Class.cast` y no por `(E)`: el borrado haria que un cast escrito no se
    // comprobara aca sino recien donde el llamador use el elemento, y ahi el ClassCastException
    // apuntaria al lugar equivocado. `Class.cast` falla en el filtro, que es donde esta el error.
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

    // `LinkedHashSet` y no `HashSet`: el contrato promete conservar el orden de iteracion de la
    // entrada, y eso es justamente lo que un HashSet perderia.
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

    /** Las directivas `exports` de un modulo. */
    public static List<ModuleElement.ExportsDirective> exportsIn(
            Iterable<? extends ModuleElement.Directive> directives) {
        return listFilter(directives, ModuleElement.DirectiveKind.EXPORTS,
                ModuleElement.ExportsDirective.class);
    }

    /** Las directivas `opens`. */
    public static List<ModuleElement.OpensDirective> opensIn(
            Iterable<? extends ModuleElement.Directive> directives) {
        return listFilter(directives, ModuleElement.DirectiveKind.OPENS,
                ModuleElement.OpensDirective.class);
    }

    /** Las directivas `provides`. */
    public static List<ModuleElement.ProvidesDirective> providesIn(
            Iterable<? extends ModuleElement.Directive> directives) {
        return listFilter(directives, ModuleElement.DirectiveKind.PROVIDES,
                ModuleElement.ProvidesDirective.class);
    }

    /** Las directivas `requires`. */
    public static List<ModuleElement.RequiresDirective> requiresIn(
            Iterable<? extends ModuleElement.Directive> directives) {
        return listFilter(directives, ModuleElement.DirectiveKind.REQUIRES,
                ModuleElement.RequiresDirective.class);
    }

    /** Las directivas `uses`. */
    public static List<ModuleElement.UsesDirective> usesIn(
            Iterable<? extends ModuleElement.Directive> directives) {
        return listFilter(directives, ModuleElement.DirectiveKind.USES,
                ModuleElement.UsesDirective.class);
    }

    // Las directivas se filtran por igualdad de kind y no por un conjunto: cada una de las cinco
    // tiene exactamente un kind, y un `==` sobre un enum dice lo mismo sin construir nada.
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
