package javax.xml.crypto;

/**
 * KajiLibrary's javax.xml.crypto.XMLStructure -- cualquier pedazo de una estructura criptografica
 * XML.
 *
 * <p>La interfaz raiz del paquete, y casi vacia a proposito: un solo metodo, que pregunta si una
 * caracteristica esta soportada. Todo lo demas --que sea una firma, una referencia, una clave-- lo
 * dicen las subinterfaces.
 *
 * <p>Existe porque las estructuras de XML-DSig se anidan de formas que no se pueden tipar de
 * antemano: el contenido de un {@code Object} o de un {@code KeyInfo} es "lo que sea que el
 * documento traiga", y eso necesita un tipo comun.
 *
 * <p>{@link #isFeatureSupported} recibe un nombre de caracteristica --como los de un
 * {@code XMLReader}-- y devuelve false para las que no conoce. No hay una lista estandar; cada
 * implementacion define las suyas.
 */
public interface XMLStructure {

    /**
     * Si esta implementacion soporta esa caracteristica.
     *
     * @throws NullPointerException si el nombre es null
     */
    boolean isFeatureSupported(String feature);
}
