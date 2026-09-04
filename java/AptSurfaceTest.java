// Prueba de comportamiento de `javax.annotation.processing`.
//
// Esta escrita para correr **igual en las dos VMs**: la de KajiJDK y el `java` real. Por eso toca
// solo tipos de la especificacion (nada de `KajiFiler`, `ProcessingEnvironmentImpl` ni `AptMessager`,
// que son de esta casa) y no depende de `javax.lang.model.util`, que KajiLibrary todavia no tiene.
// Correrla de los dos lados es el unico modo de saber si lo que escribimos es la semantica del JDK o
// una invencion nuestra que casualmente pasa en nuestra VM.
//
// Las tres cosas que de verdad se verifican, y que son las que un `AbstractProcessor` puede romper:
//
//   1. El **protocolo de `init`**: una sola vez, no acepta `null`, y `isInitialized()` refleja si
//      paso o no. Un procesador reinicializado se queda con el `Filer` de otra corrida.
//   2. Los **metodos `default`** de `Messager` y `RoundEnvironment` despachan contra `this`: los seis
//      atajos tienen que terminar en el `printMessage` del implementador con el `Kind` correcto, y
//      la union de `getElementsAnnotatedWithAny` tiene que llamar a la busqueda de a una por cada
//      anotacion que le pasan. Es logica que vive en la interfaz, asi que si esta mal, esta mal para
//      todos los implementadores a la vez.
//   3. Que `Completions.of(v)` deje el mensaje en `""` y no en `null`.
//
// AVISO — lo que esta prueba deliberadamente NO comprueba: si `getSupportedAnnotationTypes()` lee su
// `@SupportedAnnotationTypes`. En nuestra VM no puede (nuestro javac no emite
// `RuntimeVisibleAnnotations` para anotaciones que vienen del classpath; ver el encabezado de
// `AbstractProcessor`), y en el `java` real si. Afirmar cualquiera de los dos haria fallar la prueba
// del otro lado, y taparlo con un `if` seria esconder justo el hallazgo. Se comprueba el caso SIN
// anotacion, que es identico en las dos, y la divergencia se explica en el informe.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.Completions;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

public class AptSurfaceTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    // ---- Completion / Completions ----

    static void completados() {
        Completion c = Completions.of("valor", "explicacion");
        ok(c.getValue().equals("valor"));
        ok(c.getMessage().equals("explicacion"));

        // La forma de un argumento deja el mensaje en la cadena vacia, NO en null: es la diferencia
        // que obliga (o no) a todo consumidor a chequear.
        Completion s = Completions.of("solo");
        ok(s.getValue().equals("solo"));
        ok(s.getMessage() != null);
        ok(s.getMessage().equals(""));

        // Cada llamada da un objeto nuevo; la fabrica no cachea.
        ok(Completions.of("a") != Completions.of("a"));
    }

    // ---- FilerException ----

    static void filerException() {
        FilerException e = new FilerException("ya existe");
        ok(e.getMessage().equals("ya existe"));
        // Es una IOException, y eso es del contrato: generar hace E/S y quien genera tiene que
        // decidir que hacer si no puede escribir.
        ok(e instanceof IOException);
    }

    // ---- Los seis metodos `default` de Messager ----

    // Un Messager que no imprime: anota el ultimo Kind y si vino con elemento. Implementa solo las
    // cuatro abstractas, que es justamente el punto — los seis atajos los tiene que poner la
    // interfaz.
    static class MessagerEspia implements Messager {
        Diagnostic.Kind ultimoKind;
        boolean conElemento;
        int llamadas;

        public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
            this.ultimoKind = kind;
            this.conElemento = false;
            this.llamadas = this.llamadas + 1;
        }

        public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {
            this.ultimoKind = kind;
            this.conElemento = true;
            this.llamadas = this.llamadas + 1;
        }

        public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e,
                AnnotationMirror a) {
            this.ultimoKind = kind;
            this.conElemento = true;
            this.llamadas = this.llamadas + 1;
        }

        public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e,
                AnnotationMirror a, AnnotationValue v) {
            this.ultimoKind = kind;
            this.conElemento = true;
            this.llamadas = this.llamadas + 1;
        }
    }

    static void atajosDeMessager() {
        MessagerEspia m = new MessagerEspia();

        m.printError("x");
        ok(m.ultimoKind == Diagnostic.Kind.ERROR);
        ok(!m.conElemento);

        m.printWarning("x");
        ok(m.ultimoKind == Diagnostic.Kind.WARNING);
        ok(!m.conElemento);

        m.printNote("x");
        ok(m.ultimoKind == Diagnostic.Kind.NOTE);
        ok(!m.conElemento);

        // Las variantes con Element tienen que caer en la sobrecarga de tres argumentos, no en la
        // de dos: si el atajo perdiera el elemento, el subrayado se iria al archivo entero.
        m.printError("x", null);
        ok(m.ultimoKind == Diagnostic.Kind.ERROR);
        ok(m.conElemento);

        m.printWarning("x", null);
        ok(m.ultimoKind == Diagnostic.Kind.WARNING);
        ok(m.conElemento);

        m.printNote("x", null);
        ok(m.ultimoKind == Diagnostic.Kind.NOTE);
        ok(m.conElemento);

        // Seis atajos, seis despachos: ninguno se comio la llamada.
        ok(m.llamadas == 6);
    }

    // ---- Los dos metodos `default` de RoundEnvironment ----

    // Cuenta cuantas veces le preguntaron de a una. No fabrica Elements (no hay como, sin el modelo
    // reificado), y no hace falta: lo que se prueba es el despacho, no el contenido.
    static class RondaEspia implements RoundEnvironment {
        int porTypeElement;
        int porClase;

        public boolean processingOver() { return false; }

        public boolean errorRaised() { return false; }

        public Set<? extends Element> getRootElements() { return new HashSet<Element>(); }

        public Set<? extends Element> getElementsAnnotatedWith(TypeElement a) {
            this.porTypeElement = this.porTypeElement + 1;
            return new HashSet<Element>();
        }

        public Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> a) {
            this.porClase = this.porClase + 1;
            return new HashSet<Element>();
        }
    }

    static void unionDeRoundEnvironment() {
        RondaEspia r = new RondaEspia();

        // Tres anotaciones -> tres busquedas de a una. La union se define enteramente en terminos de
        // la busqueda simple, asi que si el default no itera, un procesador que pide varias
        // anotaciones a la vez recibe de menos y no se entera.
        Set<? extends Element> u = r.getElementsAnnotatedWithAny(
                new TypeElement[] { null, null, null });
        ok(r.porTypeElement == 3);
        ok(u != null);
        ok(u.isEmpty());

        // Sin anotaciones no se pregunta nada, y el resultado es vacio (no null).
        RondaEspia r2 = new RondaEspia();
        Set<? extends Element> vacio = r2.getElementsAnnotatedWithAny(new TypeElement[0]);
        ok(r2.porTypeElement == 0);
        ok(vacio.isEmpty());

        // La sobrecarga por Class va a la busqueda por Class, no a la de TypeElement.
        RondaEspia r3 = new RondaEspia();
        Set<Class<? extends Annotation>> clases = new HashSet<Class<? extends Annotation>>();
        clases.add(Deprecated.class);
        clases.add(Override.class);
        r3.getElementsAnnotatedWithAny(clases);
        ok(r3.porClase == 2);
        ok(r3.porTypeElement == 0);
    }

    // ---- AbstractProcessor ----

    // Sin ninguna anotacion de soporte encima: es el caso que las dos VMs contestan igual.
    static class ProcSinAnotar extends AbstractProcessor {
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            return false;
        }

        // `isInitialized()` es `protected` y `AbstractProcessor` esta en otro paquete: por JLS 6.6.2
        // solo se puede tocar desde el cuerpo de una subclase, y `AptSurfaceTest` no lo es (la
        // subclase es esta clase anidada). Asi que el puente va aca adentro, que si es legal.
        //
        // No es un detalle de estilo: escrito directo como `p.isInitialized()` desde el metodo de
        // afuera, NUESTRO javac lo acepta y el javac real lo rechaza — y el `.class` que sale
        // revienta con `IllegalAccessError` al correrlo en la JVM real. Es un hallazgo, y esta en el
        // informe; el puente es la forma correcta de escribirlo.
        public boolean inicializado() {
            return this.isInitialized();
        }
    }

    static void procesadorAbstracto() {
        ProcSinAnotar p = new ProcSinAnotar();

        // Antes de init no esta inicializado, y las tres `getSupported*` ya contestan igual (el
        // contrato permite preguntar antes de init).
        ok(!p.inicializado());
        ok(p.getSupportedAnnotationTypes().isEmpty());
        ok(p.getSupportedOptions().isEmpty());

        // RELEASE_6 y no `latest()`: es el default que fija el contrato, deliberadamente bajo para
        // que un procesador que no dice nada no prometa entender lo que no conoce.
        ok(p.getSupportedSourceVersion() == SourceVersion.RELEASE_6);

        // Sin sugerencias, pero un iterable de verdad y no null.
        Iterable<? extends Completion> comp = p.getCompletions(null, null, null, "");
        ok(comp != null);
        ok(!comp.iterator().hasNext());

        // `init(null)` es NPE, y lo importante es que **no** deja el procesador medio inicializado.
        try {
            p.init(null);
            ok(false);
        } catch (NullPointerException e) {
            ok(true);
        }
        ok(!p.inicializado());
    }

    // El protocolo de init necesita un ProcessingEnvironment de verdad. No se puede implementar la
    // interfaz aca (el JDK real le pide ademas `getElementUtils`/`getTypeUtils`, que dependen de
    // `javax.lang.model.util`, un paquete que KajiLibrary no tiene: una clase que implemente la
    // interfaz completa no compila de este lado, y una que implemente la nuestra no compila del
    // otro). Entonces se prueba lo que si es portable: que el estado `initialized` sea privado al
    // objeto y no compartido entre instancias.
    static void initEsPorInstancia() {
        ProcSinAnotar a = new ProcSinAnotar();
        ProcSinAnotar b = new ProcSinAnotar();
        ok(!a.inicializado());
        ok(!b.inicializado());

        // Un fallo de init en uno no toca al otro (el campo es de instancia, no estatico).
        try {
            a.init(null);
        } catch (NullPointerException e) {
            // esperado
        }
        ok(!a.inicializado());
        ok(!b.inicializado());
    }

    // ---- Las anotaciones de soporte existen y se pueden nombrar ----

    static void anotacionesDeSoporte() {
        // Que sean tipos anotacion de verdad, no interfaces comunes.
        ok(javax.annotation.processing.SupportedAnnotationTypes.class.isAnnotation());
        ok(javax.annotation.processing.SupportedOptions.class.isAnnotation());
        ok(javax.annotation.processing.SupportedSourceVersion.class.isAnnotation());
        ok(javax.annotation.processing.Generated.class.isAnnotation());
    }

    public static int run() {
        completados();
        filerException();
        atajosDeMessager();
        unionDeRoundEnvironment();
        procesadorAbstracto();
        initEsPorInstancia();
        anotacionesDeSoporte();
        return primerFallo;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
