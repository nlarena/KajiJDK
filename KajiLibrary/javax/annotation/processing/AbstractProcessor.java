package javax.annotation.processing;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// La clase base de la que hereda casi todo procesador real (JSR 269 §AbstractProcessor). Lo unico
// que deja abstracto es `process`; todo lo demas lo resuelve leyendo por reflexion las anotaciones
// `@SupportedAnnotationTypes`, `@SupportedOptions` y `@SupportedSourceVersion` de la subclase.
//
// ============================================================================================
//  AVISO IMPORTANTE — las tres `getSupported*` devuelven su valor por defecto en ESTA VM
// ============================================================================================
//
// El mecanismo de esta clase es leer sus propias anotaciones en tiempo de ejecucion. En KajiJDK eso
// **no funciona todavia**, y no por como esta escrita esta clase sino por un bug del compilador del
// proyecto:
//
//   nuestro javac no emite el atributo `RuntimeVisibleAnnotations` cuando el tipo de la anotacion
//   que se aplica se resuelve desde el **classpath** (un `.class`), en vez de estar declarado en la
//   misma unidad de compilacion.
//
// Un procesador de usuario esta siempre en ese caso: `@SupportedAnnotationTypes` vive en
// KajiLibrary, o sea en el classpath. Asi que su `.class` sale sin la anotacion, y
// `getClass().getAnnotation(SupportedAnnotationTypes.class)` devuelve `null` — verificado, no
// supuesto. (El mismo bug explica por que el propio `SupportedAnnotationTypes.class` de la
// biblioteca perdio su `@Retention(RUNTIME)`: se compilo leyendo `@Retention` del classpath.)
//
// Consecuencia concreta: `getSupportedAnnotationTypes()` devuelve el conjunto vacio,
// `getSupportedOptions()` tambien, y `getSupportedSourceVersion()` devuelve `RELEASE_6`, para
// **cualquier** procesador, tenga o no las anotaciones puestas.
//
// Se escribio igual con la logica real, y a proposito: el codigo es correcto — hace exactamente lo
// que manda el contrato con la entrada que recibe — y va a empezar a dar la respuesta buena sola en
// cuanto el compilador emita las anotaciones. Falsear el resultado (por ejemplo devolver `{"*"}`)
// seria mentir sobre lo que el procesador declaro.
//
// Que esto no rompa nada hoy tiene una razon puntual: el round loop de este proyecto
// (`src/jvm/interpreter/apt.rs`) no consulta ninguna de las tres — construye el procesador, le da
// `init(env)` y lo llama a `process(...)` en cada ronda. El filtrado por tipo de anotacion todavia
// no existe, asi que un procesador recibe todas las rondas igual.
//
// UNA OMISION DELIBERADA, por lo mismo. El JDK real, cuando no encuentra la anotacion y el
// procesador ya esta inicializado, avisa por el `Messager`: "No SupportedSourceVersion annotation
// found on X, returning RELEASE_6". Aca ese aviso **no** se emite, y a proposito: como la anotacion
// nunca se ve, el aviso saldria para todo procesador — incluidos los que SI la tienen puesta. Seria
// decirle a alguien que se olvido de algo que en realidad escribio. Un aviso que miente es peor que
// no avisar; la explicacion honesta es este encabezado.
public abstract class AbstractProcessor implements Processor {

    /** El entorno que dejo `init`. `protected` porque las subclases lo usan directo. */
    protected ProcessingEnvironment processingEnv;

    // Si ya paso por `init`. Se lee y escribe bajo el candado de la instancia (ver `init` y
    // `isInitialized`, ambos `synchronized`): la herramienta puede inicializar en un hilo y
    // procesar en otro.
    private boolean initialized = false;

    /** Solo para las subclases. */
    protected AbstractProcessor() {
    }

    /**
     * Las opciones de `@SupportedOptions`, o el conjunto vacio si no esta.
     *
     * <p>Ver el aviso del encabezado: hoy siempre es el conjunto vacio.
     */
    public Set<String> getSupportedOptions() {
        SupportedOptions so = this.getClass().getAnnotation(SupportedOptions.class);
        if (so == null) {
            return Collections.emptySet();
        }
        return arrayToSet(so.value());
    }

    /**
     * Los tipos de `@SupportedAnnotationTypes`, o el conjunto vacio si no esta.
     *
     * <p>Ver el aviso del encabezado: hoy siempre es el conjunto vacio.
     */
    public Set<String> getSupportedAnnotationTypes() {
        SupportedAnnotationTypes sat = this.getClass().getAnnotation(SupportedAnnotationTypes.class);
        if (sat == null) {
            return Collections.emptySet();
        }
        return arrayToSet(sat.value());
    }

    /**
     * La version de `@SupportedSourceVersion`, o `RELEASE_6` si no esta.
     *
     * <p>`RELEASE_6` y no `latest()`: es el default que fija el contrato, y es deliberadamente bajo
     * para que un procesador que no dice nada no prometa entender construcciones que no conoce. Ver
     * el aviso del encabezado: hoy siempre cae en este default.
     */
    public SourceVersion getSupportedSourceVersion() {
        SupportedSourceVersion ssv = this.getClass().getAnnotation(SupportedSourceVersion.class);
        if (ssv == null) {
            return SourceVersion.RELEASE_6;
        }
        return ssv.value();
    }

    /**
     * Guarda el entorno. `synchronized`, y rechaza el segundo llamado: el contrato dice "exactamente
     * una vez", y un procesador reinicializado a mitad de camino quedaria con un `Filer` de otra
     * corrida.
     *
     * @throws IllegalStateException si ya se llamo
     */
    public synchronized void init(ProcessingEnvironment processingEnv) {
        if (this.initialized) {
            throw new IllegalStateException("Cannot call init more than once.");
        }
        Objects.requireNonNull(processingEnv, "Tool provided null ProcessingEnvironment");
        this.processingEnv = processingEnv;
        this.initialized = true;
    }

    /** Lo unico que la subclase tiene que escribir. */
    public abstract boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv);

    /**
     * Sin sugerencias. Es la respuesta correcta para un procesador que no ofrece completado, y la
     * que da el JDK real desde esta clase base.
     */
    public Iterable<? extends Completion> getCompletions(Element element,
            AnnotationMirror annotation, ExecutableElement member, String userText) {
        return SIN_COMPLETADOS;
    }

    // Se guarda en un campo con el tipo escrito en vez de devolver `Collections.emptyList()` en el
    // `return`: nuestro javac no infiere `T` cuando el destino es un supertipo con comodin
    // (`List<T>` contra `Iterable<? extends Completion>`) y rechaza la llamada. Con el tipo fijado
    // acá la inferencia no hace falta — y de paso la lista vacía se crea una sola vez.
    private static final List<Completion> SIN_COMPLETADOS =
            Collections.unmodifiableList(new ArrayList<Completion>());

    /** Si ya paso por {@link #init}. */
    protected synchronized boolean isInitialized() {
        return this.initialized;
    }

    // Interno: copia el arreglo de una anotacion a un conjunto inmutable. `LinkedHashSet` para no
    // perder el orden en que se declararon (el contrato no lo exige, pero un orden estable hace que
    // los mensajes de diagnostico no bailen), y una **copia** porque `value()` devuelve el arreglo
    // clonado de la anotacion y no queremos que el conjunto siga atado a el.
    private static Set<String> arrayToSet(String[] array) {
        Set<String> set = new LinkedHashSet<String>();
        for (String s : array) {
            set.add(s);
        }
        return Collections.unmodifiableSet(set);
    }
}
