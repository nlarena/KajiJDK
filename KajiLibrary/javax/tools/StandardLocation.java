package javax.tools;

// KajiLibrary's javax.tools.StandardLocation — the thirteen locations every standard file
// manager understands. Naming them as an enum (instead of leaving them strings) is what lets
// a compiler ask for "the class output" without agreeing on a spelling with its caller.
//
// OMITIDO (salida (a)):
//
//   - La clausula `implements JavaFileManager.Location`. El javac congelado no puede nombrar
//     un tipo anidado declarado en otra unidad de compilacion: `JavaFileManager.Location` da
//     error duro, y con `import javax.tools.JavaFileManager.Location` la clausula `implements`
//     se descarta EN SILENCIO (el .class sale sin la superinterfaz y sin aviso). Antes que
//     una superinterfaz fantasma, la ausencia declarada. Los tres metodos del contrato
//     (getName / isOutputLocation / isModuleOrientedLocation) SI estan, con la firma exacta,
//     asi que el dia que el compilador resuelva anidados alcanza con agregar la clausula.
//
//   - `public static Location locationFor(String)`. Su tipo de retorno es ese mismo anidado;
//     devolverlo como StandardLocation seria una firma falsa.
public enum StandardLocation {

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
