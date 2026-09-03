package javax.xml.namespace;

import java.util.Iterator;

import javax.xml.XMLConstants;

/**
 * KajiLibrary's javax.xml.namespace.NamespaceContext -- el diccionario prefijo/URI que vale en un
 * punto del documento.
 *
 * <p>Las declaraciones {@code xmlns} de XML tienen alcance lexico: valen en el elemento que las
 * declara y en todo lo que cuelga de el, y una declaracion de adentro tapa a la de afuera. Un
 * {@code NamespaceContext} es esa pila de declaraciones **vista desde una posicion**; quien lo
 * implementa es el parser, que sabe donde esta parado.
 *
 * <h2>La relacion no es uno a uno, y por eso hay tres metodos</h2>
 *
 * <p>Un prefijo apunta a un solo URI --si no, no se podria resolver un nombre-- pero un URI puede
 * tener varios prefijos ligados a la vez:
 *
 * <pre>{@code
 * <raiz xmlns:a="http://tienda" xmlns:b="http://tienda"/>
 * }</pre>
 *
 * <p>De ahi la asimetria de la interfaz: {@link #getNamespaceURI} devuelve **el** URI,
 * {@link #getPrefix} devuelve **un** prefijo cualquiera de los ligados, y {@link #getPrefixes} los
 * devuelve todos. Quien tenga que volver a escribir el documento usa el segundo; quien tenga que
 * decidir si dos nombres son el mismo usa el primero.
 *
 * <h2>Las ligaduras que no se pueden cambiar</h2>
 *
 * <p>Tres pares estan fijados por la especificacion y toda implementacion los tiene que contestar
 * aunque el documento no los declare, porque no son declarables:
 *
 * <ul>
 *   <li>{@code xml} -&gt; {@link XMLConstants#XML_NS_URI};
 *   <li>{@code xmlns} -&gt; {@link XMLConstants#XMLNS_ATTRIBUTE_NS_URI};
 *   <li>el prefijo por omision, cuando no hay {@code xmlns=} vigente, -&gt;
 *       {@link XMLConstants#NULL_NS_URI}.
 * </ul>
 *
 * <h2>Que hay aca</h2>
 *
 * <p>Los tres metodos, que es toda la interfaz. No hay implementacion concreta en esta biblioteca
 * porque no la hay en el JDK tampoco: un contexto sin un parser que lo alimente no tiene de donde
 * sacar declaraciones, asi que la clase util es la que escribe cada implementacion de StAX o de DOM
 * con la pila que ya lleva.
 */
public interface NamespaceContext {

    /**
     * El espacio de nombres ligado a {@code prefix} en esta posicion.
     *
     * <p>Nunca devuelve null: un prefijo sin ligar da {@link XMLConstants#NULL_NS_URI}, la cadena
     * vacia. Que devuelva la cadena vacia y no null es lo que hace que el resultado se pueda pasar
     * derecho a un constructor de {@link QName} sin chequear.
     *
     * @param prefix el prefijo a resolver; la cadena vacia pregunta por el espacio de nombres por
     *     omision
     * @return el URI ligado, o la cadena vacia si no hay ninguno
     * @throws IllegalArgumentException si {@code prefix} es null
     */
    String getNamespaceURI(String prefix);

    /**
     * Un prefijo ligado a {@code namespaceURI}, o null si no hay ninguno.
     *
     * <p>Cual de los ligados devuelve no esta definido cuando hay varios, y eso es deliberado: la
     * eleccion depende de que este mas cerca en la pila, que es cosa de cada implementacion.
     *
     * <p>Devuelve null --y no la cadena vacia-- cuando no hay ninguno, al reves que
     * {@link #getNamespaceURI}. La asimetria es del contrato original y tiene su logica: la cadena
     * vacia **es** un prefijo valido (el por omision), asi que no puede significar tambien "no
     * hay".
     *
     * @param namespaceURI el espacio de nombres a buscar
     * @return un prefijo ligado a el, o null
     * @throws IllegalArgumentException si {@code namespaceURI} es null
     */
    String getPrefix(String namespaceURI);

    /**
     * Todos los prefijos ligados a {@code namespaceURI}, en un iterador de solo lectura.
     *
     * <p>El iterador no admite {@code remove}: sacar una declaracion de espacio de nombres de un
     * documento a medio leer no significa nada.
     *
     * @param namespaceURI el espacio de nombres a buscar
     * @return los prefijos ligados; vacio si no hay ninguno, nunca null
     * @throws IllegalArgumentException si {@code namespaceURI} es null
     */
    Iterator<String> getPrefixes(String namespaceURI);
}
