package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.XMLReporter -- a donde van los avisos que **no** interrumpen la
 * lectura.
 *
 * <p>Un parser encuentra dos clases de problema y no se tratan igual. Lo que impide seguir leyendo
 * --una etiqueta sin cerrar, un caracter ilegal-- sale como {@link XMLStreamException} y corta el
 * recorrido. Lo que se puede reportar y seguir --una entidad no declarada que se resuelve vacia, un
 * atributo duplicado que se descarta-- llega aca. Sin este canal, la unica salida seria elegir entre
 * abortar por algo menor o callarse, y las dos son malas.
 *
 * <p>Que el metodo pueda lanzar {@link XMLStreamException} es lo que le devuelve el control a quien
 * escribe el reporter: un aviso que la aplicacion considere fatal se convierte en fatal lanzando
 * desde adentro.
 */
public interface XMLReporter {

    /**
     * Reporta un problema no fatal.
     *
     * @param message el texto del aviso
     * @param errorType el tipo de problema, definido por la implementacion del parser
     * @param relatedInformation lo que el parser tenga a mano sobre el caso, o null
     * @param location donde paso, o null si no se sabe
     * @throws XMLStreamException si quien reporta decide que este aviso si tiene que cortar
     */
    void report(String message, String errorType, Object relatedInformation, Location location)
            throws XMLStreamException;
}
