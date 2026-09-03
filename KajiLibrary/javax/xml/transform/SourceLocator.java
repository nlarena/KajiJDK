package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.SourceLocator -- donde, dentro de un documento, paso algo.
 *
 * <p>Es la contraparte de `org.xml.sax.Locator` para el mundo de las transformaciones, y existe por
 * una razon muy concreta: un error de XSLT ocurre en **dos** lugares a la vez --una linea de la hoja
 * de estilo y un nodo del documento-- y un mensaje que solo diga "elemento inesperado" no sirve para
 * arreglar nada. Esta interfaz es el minimo comun para poder decir *donde*.
 *
 * <p>Las cuatro coordenadas son las mismas de SAX y con las mismas convenciones, que conviene
 * recordar porque no son obvias:
 *
 * <ul>
 *   <li>las URIs (`publicId`, `systemId`) pueden ser nulas: no todo documento vino de un lugar con
 *       nombre --uno armado en memoria no tiene ninguno--;
 *   <li>las posiciones (`line`, `column`) se cuentan **desde 1**, y el **0 significa "no se"**. No
 *       hay linea cero, asi que el valor sirve de centinela sin necesidad de un `Integer` nulo. Por
 *       eso {@link TransformerException#getLocationAsString} omite del texto la linea y la columna
 *       que valgan cero: informar "Line#: 0" seria peor que callarse.
 * </ul>
 *
 * <p>Y una advertencia que la spec hace y vale repetir: un `SourceLocator` que un procesador entrega
 * durante el recorrido es **valido solo durante la llamada**. El procesador puede reutilizar el
 * mismo objeto y moverlo. Guardarlo para mirarlo despues da coordenadas de otro lado; lo que se
 * guarda es una copia de los cuatro valores.
 */
public interface SourceLocator {

    /** El identificador publico del documento, o null si no tiene. */
    String getPublicId();

    /** La URI del documento, o null si no vino de ninguna. */
    String getSystemId();

    /** La linea, contada desde 1; 0 si se desconoce. */
    int getLineNumber();

    /** La columna, contada desde 1; 0 si se desconoce. */
    int getColumnNumber();
}
