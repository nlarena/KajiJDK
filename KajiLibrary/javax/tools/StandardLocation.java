package javax.tools;

// KajiLibrary's javax.tools.StandardLocation — the thirteen locations every standard file
// manager understands. Naming them as an enum (instead of leaving them strings) is what lets
// a compiler ask for "the class output" without agreeing on a spelling with its caller.
//
// La clausula `implements JavaFileManager.Location` **esta**. La nota anterior explicaba que se
// habia omitido porque el javac congelado no podia nombrar un tipo anidado de otra unidad -- con el
// nombre calificado daba error duro y con `import` la clausula se descartaba **en silencio**, o sea
// una superinterfaz fantasma. Eso se arreglo, y la nota decia que ese dia alcanzaba con agregar la
// clausula: alcanzo.
//
// Con ella entra tambien `locationFor(String)`, cuyo retorno es ese mismo tipo anidado.
public enum StandardLocation implements JavaFileManager.Location {

    CLASS_OUTPUT,
    SOURCE_OUTPUT,
    CLASS_PATH,
    SOURCE_PATH,
    ANNOTATION_PROCESSOR_PATH,
    ANNOTATION_PROCESSOR_MODULE_PATH,
    PLATFORM_CLASS_PATH,
    NATIVE_HEADER_OUTPUT,
    MODULE_SOURCE_PATH,
    UPGRADE_MODULE_PATH,
    SYSTEM_MODULES,
    MODULE_PATH,
    PATCH_MODULE_PATH;

    /**
     * La ubicacion de ese nombre, creando una nueva si no es una de las estandar.
     *
     * <p>Que pueda **crear** una es el punto: las ubicaciones no son un conjunto cerrado, y una
     * herramienta puede definir la suya. Las creadas se recuerdan, para que dos llamadas con el
     * mismo nombre den la **misma** ubicacion -- si no, un `Map` con ubicaciones por clave nunca
     * encontraria nada.
     */
    public static JavaFileManager.Location locationFor(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        StandardLocation[] estandar = StandardLocation.values();
        int i = 0;
        while (i < estandar.length) {
            if (estandar[i].getName().equals(name)) {
                return estandar[i];
            }
            i = i + 1;
        }
        synchronized (CREADAS) {
            JavaFileManager.Location ya = CREADAS.get(name);
            if (ya != null) {
                return ya;
            }
            JavaFileManager.Location nueva = new UbicacionPropia(name);
            CREADAS.put(name, nueva);
            return nueva;
        }
    }

    // Las ubicaciones que `locationFor` invento. Un mapa y no una lista porque la pregunta es
    // siempre "la de este nombre".
    private static final java.util.HashMap<String, JavaFileManager.Location> CREADAS =
            new java.util.HashMap<String, JavaFileManager.Location>();

    public String getName() {
        return name();
    }

    // Las tres a las que el compilador ESCRIBE.
    public boolean isOutputLocation() {
        return this == CLASS_OUTPUT || this == SOURCE_OUTPUT || this == NATIVE_HEADER_OUTPUT;
    }

    // El JDK real pregunta si el nombre contiene "MODULE"; sin String.contains en la
    // biblioteca, la lista va enumerada — que es la misma respuesta, constante por constante.
    public boolean isModuleOrientedLocation() {
        return this == ANNOTATION_PROCESSOR_MODULE_PATH
            || this == MODULE_SOURCE_PATH
            || this == UPGRADE_MODULE_PATH
            || this == SYSTEM_MODULES
            || this == MODULE_PATH
            || this == PATCH_MODULE_PATH;
    }
}

// La ubicacion que `StandardLocation.locationFor` inventa para un nombre que no es de las estandar.
// Es de nivel superior y de paquete: el JDK la tiene anidada y anonima, y aca una clase con nombre
// se lee mejor y no depende de la captura del entorno.
//
// **No es de salida.** Una ubicacion inventada no puede saberlo, y decir que si haria que una
// herramienta intentara escribir en ella.
final class UbicacionPropia implements JavaFileManager.Location {

    private final String nombre;

    UbicacionPropia(String nombre) {
        this.nombre = nombre;
    }

    public String getName() {
        return this.nombre;
    }

    public boolean isOutputLocation() {
        return false;
    }

    public String toString() {
        return this.nombre;
    }
}
