package javax.annotation.processing;

import javax.lang.model.SourceVersion;

import java.util.Locale;
import java.util.Map;

// El unico canal por el que un procesador habla con la herramienta que lo corre (JSR 269). Se lo
// entrega `Processor.init(env)` una sola vez, y de ahi salen todos los servicios: el `Filer` para
// generar, el `Messager` para reportar, las opciones y el idioma.
//
// QUE FALTA Y POR QUE — el contrato real tiene tambien `getElementUtils()` y `getTypeUtils()`, que
// devuelven `javax.lang.model.util.Elements` y `javax.lang.model.util.Types`. Ese paquete
// (`javax.lang.model.util`) **no existe todavia en KajiLibrary**, y declarar los metodos con un tipo
// de retorno inexistente no compila. Se dejan afuera hasta que ese paquete este: un miembro que
// falta es un subconjunto legal.
//
// El implementador de este proyecto es `ProcessingEnvironmentImpl`.
public interface ProcessingEnvironment {

    /**
     * Las opciones `-Aclave=valor` que recibio la herramienta. Una opcion sin `=` mapea a `null`,
     * que no es lo mismo que ausente: la diferencia entre "-Adebug" y no pasarla.
     */
    Map<String, String> getOptions();

    /** Por donde reportar. */
    Messager getMessager();

    /** Por donde generar. */
    Filer getFiler();

    /** La version del lenguaje de los fuentes de esta corrida. */
    SourceVersion getSourceVersion();

    /** El idioma en el que conviene escribir los mensajes, o `null` si no hay uno. */
    Locale getLocale();

    /**
     * Si la corrida tiene las features en preview habilitadas.
     *
     * <p>`default` y no abstracto en el contrato: se agrego despues de que existieran
     * implementaciones, y "no" es la respuesta conservadora correcta para cualquiera que no sepa.
     */
    default boolean isPreviewEnabled() {
        return false;
    }

    /**
     * Las utilidades para consultar **elementos**.
     *
     * <p>Estaban afuera hasta ahora, y no por decision: devuelven
     * {@link javax.lang.model.util.Elements}, que no existia en esta biblioteca. Un metodo cuyo tipo
     * de retorno hay que sustituir por otro es otro metodo con el nombre correcto puesto encima, asi
     * que quedaba sin declarar. Ya existe.
     *
     * <p>Lo que el compilador entregue aca es cosa suya; la interfaz solo dice que lo entrega.
     */
    javax.lang.model.util.Elements getElementUtils();

    /** Las utilidades para consultar **tipos**. Mismo caso que {@link #getElementUtils()}. */
    javax.lang.model.util.Types getTypeUtils();
}
