package javax.xml.crypto;

/**
 * KajiLibrary's javax.xml.crypto.Data -- lo que se firma o se transforma.
 *
 * <p>Una interfaz <b>marcadora</b>, sin metodos. Los dos que importan son sus dos implementaciones:
 * {@link NodeSetData}, que es un conjunto de nodos, y {@link OctetStreamData}, que es un flujo de
 * bytes.
 *
 * <p>La division no es de comodidad: una transformacion de XML-DSig recibe una cosa y devuelve la
 * otra, y cual es cual determina si la cadena de transformaciones cierra. Una canonicalizacion
 * convierte nodos en bytes; una de XPath convierte nodos en nodos. Encadenar dos que no encajan es el
 * error mas comun al armar una firma a mano, y el tipo lo hace visible.
 */
public interface Data {
}
