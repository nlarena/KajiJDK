package javax.annotation.processing;

import javax.lang.model.SourceVersion;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

// Soporte del round loop de APT (JSR 269): una implementación concreta de ProcessingEnvironment que
// el driver de la VM reifica para pasarle al `init(env)` del processor. `getFiler()` entrega un
// KajiFiler (fase 4), la pieza que un processor usa para fabricar los fuentes que genera: cada
// `createSourceFile` empuja el (nombre, StringWriter) por el puente nativo y el round loop lo drena
// para reincorporar lo generado. `getMessager()` entrega un AptMessager, que escribe por el mismo
// puente nativo que AptTrace.
//
// Los tres accesorios restantes contestan lo que este compilador realmente sabe, no un placeholder:
//
//   - `getOptions()` — el mapa vacío e inmutable. El round loop no recibe opciones `-A` (el driver
//     de `apt.rs` no las parsea), así que "no hay ninguna" es la verdad. Inmutable porque el
//     contrato no permite que un procesador agregue opciones.
//   - `getSourceVersion()` — `latest()`, o sea RELEASE_25: es la versión que este javac compila.
//   - `getLocale()` — `null`, que el contrato define como "no hay locale", y es exactamente el caso
//     (no hay ninguna opción `-locale` que lo fije). Devolver `Locale.getDefault()` inventaría una
//     preferencia que nadie expresó.
public class ProcessingEnvironmentImpl implements ProcessingEnvironment {

    public Messager getMessager() { return new AptMessager(); }

    public Filer getFiler() { return new KajiFiler(); }

    public Map<String, String> getOptions() { return Collections.emptyMap(); }

    public SourceVersion getSourceVersion() { return SourceVersion.latest(); }

    public Locale getLocale() { return null; }

    // Los dos que consultan el modelo del compilador devuelven `null`, y es la respuesta honesta: no
    // hay implementacion de `Elements`/`Types` en esta biblioteca, porque lo que hace falta es la
    // tabla de simbolos y la tabla de tipos de `javac`, que viven en `src/javac/` y no en Java.
    //
    // `null` y no una implementacion vacia: un `Elements` cuyo `getTypeElement` devolviera siempre
    // `null` diria "ese tipo no existe" de cualquier tipo que se le pregunte, y un procesador leeria
    // eso como una respuesta. `null` aca dice "no hay modelo", que es lo que pasa, y el procesador
    // se topa con el problema en el lugar donde esta.
    //
    // El dia que `apt.rs` exponga el modelo, estos dos son los dos puntos donde se engancha.

    public javax.lang.model.util.Elements getElementUtils() {
        return null;
    }

    public javax.lang.model.util.Types getTypeUtils() {
        return null;
    }
}
