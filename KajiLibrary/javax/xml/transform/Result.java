package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.Result -- a donde va un documento XML.
 *
 * <p>El espejo de {@link Source}, con la misma idea: el procesador escribe sin saber si del otro lado
 * hay un archivo, un arbol o un manejador de eventos.
 *
 * <p>Las dos constantes son instrucciones de procesamiento que se **escriben dentro del documento**
 * para pedirle al serializador que deje de escapar `&lt;` y `&amp;`. Es una via de escape fea y
 * necesaria: sirve para emitir marcado ya armado, y usarla mal produce XML invalido sin que nadie
 * avise.
 */
public interface Result {

    /** Instruccion que apaga el escapado de la salida. */
    String PI_DISABLE_OUTPUT_ESCAPING = "javax.xml.transform.disable-output-escaping";

    /** Instruccion que lo vuelve a encender. */
    String PI_ENABLE_OUTPUT_ESCAPING = "javax.xml.transform.enable-output-escaping";

    /** La URI base del destino. */
    void setSystemId(String systemId);

    String getSystemId();
}
