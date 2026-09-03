package javax.annotation.processing;

import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import java.io.IOException;

// Por donde un procesador **crea** los archivos que genera (JSR 269 §Filer). No es un
// `JavaFileManager` de proposito general: la herramienta se queda con lo que se cree aca para
// reincorporarlo (una fuente generada dispara otra ronda) y para no dejar que dos procesadores
// pisen el mismo tipo.
//
// Los `originatingElements` son varargs y pueden ir vacios: son los elementos que "causaron" el
// archivo, y sirven para invalidacion incremental. El contrato los declara opcionales
// explicitamente, asi que pasar ninguno es legal y no significa un error.
//
// El implementador de este proyecto es `KajiFiler`; solo `createSourceFile` esta soportado de
// verdad (ver su encabezado).
public interface Filer {

    /**
     * Crea una fuente `.java` nueva para el tipo `name` (nombre completo, con puntos).
     *
     * @throws FilerException si ese tipo ya se creo o el nombre no es valido
     */
    JavaFileObject createSourceFile(CharSequence name, Element... originatingElements)
            throws IOException;

    /**
     * Crea un `.class` nuevo para el tipo `name`. Generar bytecode directamente es legal pero raro:
     * lo normal es generar fuente y dejar que el compilador lo compile.
     */
    JavaFileObject createClassFile(CharSequence name, Element... originatingElements)
            throws IOException;

    /**
     * Crea un recurso auxiliar (un `.properties`, un `META-INF/services/...`) en `location`.
     *
     * @param moduleAndPkg el paquete (o `modulo/paquete`) que lo contiene; vacio para la raiz
     * @param relativeName el nombre del archivo, relativo a ese paquete
     */
    FileObject createResource(JavaFileManager.Location location, CharSequence moduleAndPkg,
            CharSequence relativeName, Element... originatingElements) throws IOException;

    /**
     * Abre un recurso **existente** para leerlo. No crea nada, y no toma `originatingElements`
     * justamente porque leer no genera.
     */
    FileObject getResource(JavaFileManager.Location location, CharSequence moduleAndPkg,
            CharSequence relativeName) throws IOException;
}
