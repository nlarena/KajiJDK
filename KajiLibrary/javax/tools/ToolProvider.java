package javax.tools;

// KajiLibrary's javax.tools.ToolProvider — how a program gets hold of the tools the platform
// ships, without naming an implementation class. It is a pure static factory: the private
// constructor is the point, there is nothing to instantiate.
//
// Los dos getters devuelven `null`, y eso NO es un stub: el contrato del JDK dice
// explicitamente que devuelven null cuando la plataforma no provee la herramienta, y KajiJDK
// no expone todavia su compilador por esta API (el javac vive en `bin/javac.exe`, no como un
// javax.tools.JavaCompiler). Un caller correcto ya tiene que chequear null.
//
// OMITIDO (salida (a), omitir el miembro):
//   - `ClassLoader getSystemToolClassLoader()` — no existe java.lang.ClassLoader en
//     KajiLibrary. Es el unico miembro publico que se cae.
public class ToolProvider {

    // Nadie instancia un ToolProvider.
    private ToolProvider() {
    }

    /**
     * El cargador desde el que se cargaron las herramientas del sistema.
     *
     * <p>Devuelve el cargador unico. En el JDK esto podia ser un cargador **aparte** --las
     * herramientas vivian en `tools.jar`, fuera del classpath de la aplicacion-- y por eso el metodo
     * existe; desde que las herramientas son un modulo mas, el JDK devuelve `null`. Aca hay un solo
     * cargador y devolverlo es lo mas informativo.
     */
    public static ClassLoader getSystemToolClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    public static JavaCompiler getSystemJavaCompiler() {
        return null;
    }

    public static DocumentationTool getSystemDocumentationTool() {
        return null;
    }
}
