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
 * KajiLibrary's javax.lang.model.util.Elements — las preguntas sobre elementos que sólo el compilador
 * puede contestar.
 *
 * <p>La contraparte de {@link Types}: donde ésa razona sobre tipos, ésta razona sobre **declaraciones**
 * — buscar una clase por su nombre, saber si un método redefine a otro, leer el comentario de
 * documentación de un elemento.
 *
 * <p>Es una **declaración pura**: la implementa el compilador y la entrega por
 * {@link javax.annotation.processing.ProcessingEnvironment#getElementUtils()}. Esta biblioteca no trae
 * implementación, porque lo que hace falta es la tabla de símbolos de `javac`, que vive en `src/javac/`.
 *
 * <h2>Sobre los cuerpos por omisión</h2>
 *
 * <p>Más de la mitad de los métodos son `default`, y sus cuerpos **son parte observable de la API**, no
 * detalle. Acá se siguieron dos reglas, y conviene que estén dichas:
 *
 * <ul>
 * <li>El que se puede **deducir de los métodos abstractos** se deduce. `getAllPackageElements` se arma
 *     con `getPackageElement`, y `getOutermostTypeElement` sube por los contenedores. Eso no inventa
 *     nada: es la misma respuesta a la que llegaría quien lo escribiera a mano.</li>
 * <li>El que necesita soporte que el modelo puede no tener --módulos, componentes de registro, el
 *     archivo de origen-- devuelve la respuesta que el contrato define para "no hay soporte":
 *     `null`, conjunto vacío, o `false`. **No tira.** Una implementación que sí lo soporte redefine.</li>
 * </ul>
 */
public interface Elements {

    /**
     * De dónde salió una declaración.
     *
     * <p>La distinción tiene consecuencias para un procesador: un miembro **sintético** --el
     * `values()` de un enum, un puente-- no está en el fuente, así que reportar un error sobre él no
     * le señala nada a nadie.
     */
    enum Origin {
        /** Está escrito en el fuente o en el `.class`. */
        EXPLICIT,
        /** La especificación obliga a que exista, aunque nadie lo escribió (`Enum.values()`). */
        MANDATED,
        /** Lo fabricó el compilador y no está en la especificación (un puente). */
        SYNTHETIC;

        /**
         * Si esta declaración es visible en el fuente.
         *
         * <p>`EXPLICIT` y `MANDATED` sí --las dos son parte del programa que el lenguaje define--;
         * `SYNTHETIC` no.
         */
        public boolean isDeclared() {
            return this != Origin.SYNTHETIC;
        }
    }

    /** La forma del comentario de documentación: `/** ... *&#47;` o una corrida de `///`. */
    enum DocCommentKind {
        /** Varias líneas que empiezan con `///`. */
        END_OF_LINE,
        /** El clásico delimitado. */
        TRADITIONAL
    }

    /** El paquete con ese nombre canónico, o `null` si no hay. */
    PackageElement getPackageElement(CharSequence name);

    /**
     * El paquete con ese nombre dentro de ese módulo, o `null`.
     *
     * <p>Por omisión `null`: un modelo sin módulos no tiene por dónde buscar. No se delega en la
     * versión de un argumento a propósito — sería contestar por otro módulo que el que se pidió.
     */
    default PackageElement getPackageElement(ModuleElement module, CharSequence name) {
        return null;
    }

    /**
     * Todos los paquetes con ese nombre, en todos los módulos.
     *
     * <p>Se deduce: sin módulos hay a lo sumo uno, el que devuelve {@link #getPackageElement}.
     */
    default Set<? extends PackageElement> getAllPackageElements(CharSequence name) {
        // El conjunto se arma siempre y se devuelve vacio si no hubo nada, en vez de salir temprano
        // con `Collections.emptySet()`: nuestro javac no infiere el argumento de tipo cuando el
        // destino es un supertipo **con comodin** (`Set<? extends X>`), y da "tipo de retorno
        // incompatible". Con la variable local escrita, resuelve.
        Set<PackageElement> out = new LinkedHashSet<PackageElement>();
        PackageElement p = this.getPackageElement(name);
        if (p != null) {
            out.add(p);
        }
        return out;
    }

    /** El tipo con ese nombre canónico, o `null`. */
    TypeElement getTypeElement(CharSequence name);

    /** El tipo con ese nombre dentro de ese módulo, o `null`. Por omisión `null`, como el paquete. */
    default TypeElement getTypeElement(ModuleElement module, CharSequence name) {
        return null;
    }

    /** Todos los tipos con ese nombre. Se deduce de {@link #getTypeElement}. */
    default Set<? extends TypeElement> getAllTypeElements(CharSequence name) {
        Set<TypeElement> out = new LinkedHashSet<TypeElement>();
        TypeElement t = this.getTypeElement(name);
        if (t != null) {
            out.add(t);
        }
        return out;
    }

    /** El módulo con ese nombre, o `null` si no hay módulos en el modelo. */
    default ModuleElement getModuleElement(CharSequence name) {
        return null;
    }

    /** Todos los módulos, o el conjunto vacío si el modelo no los tiene. */
    default Set<? extends ModuleElement> getAllModuleElements() {
        return new LinkedHashSet<ModuleElement>();
    }

    /**
     * Los valores de esa anotación, **con los que no se escribieron rellenados con su omisión**.
     *
     * <p>Es la diferencia con {@link AnnotationMirror#getElementValues()}, que devuelve sólo lo
     * escrito. Un procesador casi siempre quiere ésta: `@Retention` sin `value` explícito igual tiene
     * una política.
     */
    Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(
            AnnotationMirror a);

    /** El comentario de documentación, sin delimitadores, o `null` si no tiene. */
    String getDocComment(Element e);

    /**
     * La forma del comentario, o `null` si no tiene.
     *
     * <p>Por omisión: si hay comentario es {@link DocCommentKind#TRADITIONAL}. Es lo correcto para un
     * modelo que no distingue las dos formas — la tradicional es la que existe desde siempre.
     */
    default DocCommentKind getDocCommentKind(Element e) {
        return this.getDocComment(e) == null ? null : DocCommentKind.TRADITIONAL;
    }

    /** Si está marcado obsoleto. */
    boolean isDeprecated(Element e);

    /** De dónde salió ese elemento. Por omisión {@link Origin#EXPLICIT}. */
    default Origin getOrigin(Element e) {
        return Origin.EXPLICIT;
    }

    /** De dónde salió esa anotación. Por omisión {@link Origin#EXPLICIT}. */
    default Origin getOrigin(AnnotatedConstruct c, AnnotationMirror a) {
        return Origin.EXPLICIT;
    }

    /** De dónde salió esa directiva de módulo. Por omisión {@link Origin#EXPLICIT}. */
    default Origin getOrigin(ModuleElement m, ModuleElement.Directive directive) {
        return Origin.EXPLICIT;
    }

    /**
     * Si es un **puente**, el método sintético que el compilador agrega para que un retorno
     * covariante funcione con el borrado.
     *
     * <p>Por omisión `false`: un modelo que no fabrica puentes no tiene ninguno.
     */
    default boolean isBridge(ExecutableElement e) {
        return false;
    }

    /**
     * El nombre binario de ese tipo.
     *
     * <p>No es el canónico: un tipo anidado es `Outer$Inner` acá y `Outer.Inner` allá. Es el que hay
     * que usar para nombrar el `.class`.
     */
    Name getBinaryName(TypeElement type);

    /** El paquete que contiene a ese elemento. */
    PackageElement getPackageOf(Element e);

    /** El módulo que lo contiene, o `null` si el modelo no tiene módulos. */
    default ModuleElement getModuleOf(Element e) {
        return null;
    }

    /**
     * Todos los miembros de ese tipo, **los heredados incluidos**.
     *
     * <p>Es la diferencia con {@link Element#getEnclosedElements()}, que da sólo los declarados. La
     * herencia la tiene que resolver el compilador, y de ahí que esté acá y no en el elemento.
     */
    List<? extends Element> getAllMembers(TypeElement type);

    /**
     * El tipo de nivel superior que lo contiene, o `null` si no está dentro de ninguno.
     *
     * <p>Se deduce subiendo por los contenedores hasta el último que sea un tipo.
     */
    default TypeElement getOutermostTypeElement(Element e) {
        TypeElement ultimo = null;
        Element cur = e;
        while (cur != null) {
            if (cur instanceof TypeElement) {
                ultimo = (TypeElement) cur;
            }
            cur = cur.getEnclosingElement();
        }
        return ultimo;
    }

    /** Todas las anotaciones de ese elemento, las heredadas incluidas. */
    List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e);

    /**
     * Si `hider` oculta a `hidden`.
     *
     * <p>Ocultar no es redefinir: un campo o un método estático de una subclase **oculta** al de la
     * superclase, y cuál se usa lo decide el tipo estático. Redefinir es de los métodos de instancia,
     * y lo decide el tipo dinámico. Por eso son dos métodos distintos.
     */
    boolean hides(Element hider, Element hidden);

    /** Si `overrider`, visto como miembro de `type`, redefine a `overridden`. */
    boolean overrides(ExecutableElement overrider, ExecutableElement overridden, TypeElement type);

    /**
     * Ese valor constante escrito como una expresión de Java.
     *
     * <p>Sirve para generar código: un `char` sale como `'a'` y un `String` con sus escapes puestos,
     * de modo que el texto se pueda pegar en un fuente y compile.
     */
    String getConstantExpression(Object value);

    /** Escribe una representación de esos elementos, para depurar. */
    void printElements(Writer w, Element... elements);

    /**
     * Un {@link Name} con ese contenido.
     *
     * <p>Existe porque los `Name` del modelo se comparan por **identidad**, no con `equals`: el
     * compilador los interna. Un `Name` fabricado por otro medio no coincidiría con los del modelo.
     */
    Name getName(CharSequence cs);

    /** Si ese tipo es una interfaz funcional (§9.8). */
    boolean isFunctionalInterface(TypeElement type);

    /** Si es un módulo automático. Por omisión `false`. */
    default boolean isAutomaticModule(ModuleElement module) {
        return false;
    }

    /**
     * El cuerpo de clase de esa constante de enum, o `null` si no tiene.
     *
     * <p>Una constante de enum puede traer su propio cuerpo (`ROJO { ... }`), y en ese caso es una
     * subclase anónima. Por omisión `null`: ninguna lo tiene hasta que el modelo lo diga.
     */
    default TypeElement getEnumConstantBody(VariableElement enumConstant) {
        return null;
    }

    /** El componente de registro que ese accesor devuelve, o `null` si no es un accesor. */
    default RecordComponentElement recordComponentFor(ExecutableElement accessor) {
        return null;
    }

    /** Si es el constructor canónico de un registro. Por omisión `false`. */
    default boolean isCanonicalConstructor(ExecutableElement e) {
        return false;
    }

    /** Si es el constructor compacto de un registro. Por omisión `false`. */
    default boolean isCompactConstructor(ExecutableElement e) {
        return false;
    }

    /** El archivo de donde salió ese elemento, o `null` si no se sabe. */
    default JavaFileObject getFileObjectOf(Element e) {
        return null;
    }
}
