package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.Location -- donde, en el texto de entrada, paso lo que acaba de
 * pasar.
 *
 * <p>Existe por una sola razon: un error de XML sin numero de linea es inutil. Todo evento y toda
 * {@link XMLStreamException} pueden llevar una ubicacion, y es lo que convierte un "elemento mal
 * cerrado" en algo que se puede arreglar.
 *
 * <p>Las tres coordenadas numericas se solapan a proposito y sirven para cosas distintas: linea y
 * columna son para que las lea una persona, y el desplazamiento en caracteres es para que un editor
 * pueda posicionar el cursor sin volver a contar renglones. Las tres devuelven -1 cuando la
 * implementacion no las lleva; los dos identificadores devuelven null.
 *
 * <p>Que una implementacion no lleve la cuenta es legitimo y comun: mantener linea y columna cuesta
 * en el bucle mas caliente del parser, y hay usos --leer un documento que ya se sabe correcto-- que
 * no la pagan.
 */
public interface Location {

    /**
     * La linea, contando desde 1, o -1 si no se lleva la cuenta.
     *
     * @return el numero de linea
     */
    int getLineNumber();

    /**
     * La columna, contando desde 1, o -1 si no se lleva la cuenta.
     *
     * @return el numero de columna
     */
    int getColumnNumber();

    /**
     * Cuantos caracteres van leidos desde el principio de la entrada, o -1.
     *
     * @return el desplazamiento en caracteres
     */
    int getCharacterOffset();

    /**
     * El identificador publico de la entidad de donde salio esto, o null.
     *
     * @return el identificador publico
     */
    String getPublicId();

    /**
     * El identificador de sistema --tipicamente la URI del archivo-- o null.
     *
     * @return el identificador de sistema
     */
    String getSystemId();
}
