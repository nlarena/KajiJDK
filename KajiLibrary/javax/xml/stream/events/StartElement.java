package javax.xml.stream.events;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.stream.events.StartElement -- la apertura de un elemento con todo lo que
 * la etiqueta traia.
 *
 * <h2>El evento que carga con todo</h2>
 *
 * <p>Es el mas gordo del modelo y por una razon estructural: la etiqueta de apertura es el unico
 * lugar de XML donde pasan varias cosas a la vez --se nombra un elemento, se declaran prefijos, se
 * dan atributos-- y el modelo de eventos promete que cada evento es autosuficiente. Asi que todo
 * eso viaja adentro.
 *
 * <p>La diferencia practica con el modelo de cursor esta justo aca. Un
 * {@link javax.xml.stream.XMLStreamReader} contesta las mismas preguntas, pero solo mientras esta
 * parado en el elemento; el {@code StartElement} las sigue contestando cuando el parser ya avanzo,
 * o incluso cuando ya se cerro el archivo.
 *
 * <h2>Atributos y espacios de nombres van separados</h2>
 *
 * <p>{@link #getAttributes()} <b>no</b> incluye las declaraciones {@code xmlns}, que salen por
 * {@link #getNamespaces()}. Es la regla de la especificacion de Namespaces: una declaracion se
 * escribe como atributo pero no es uno, y confundirlos hace que un mapeo generico se lleve el
 * {@code xmlns} como si fuera un campo de datos.
 *
 * <h2>El contexto es el completo, no el local</h2>
 *
 * <p>{@link #getNamespaceContext()} devuelve el alcance <b>vigente</b> en este elemento: incluye lo
 * que declararon los ancestros, no solo lo de esta etiqueta. Es lo que hace falta para resolver un
 * prefijo que aparezca <b>dentro</b> de un valor de atributo --como en {@code xsi:type="tns:Pago"},
 * donde {@code tns} bien puede venir declarado en la raiz--, que es un caso que ningun otro accesor
 * cubre.
 */
public interface StartElement extends XMLEvent {

    /**
     * El nombre del elemento.
     *
     * <p>Un elemento sin prefijo <b>si</b> queda en el espacio de nombres por omision, al reves de
     * lo que pasa con los atributos.
     *
     * @return el nombre calificado; nunca null
     */
    QName getName();

    /**
     * Los atributos de la etiqueta, sin las declaraciones {@code xmlns}.
     *
     * <p>El orden no es significativo --XML dice que los atributos de un elemento no estan
     * ordenados-- asi que no hay que depender de el.
     *
     * @return un iterador de {@link Attribute}; vacio si no hay, nunca null
     */
    Iterator<Attribute> getAttributes();

    /**
     * Las declaraciones {@code xmlns} que hace <b>esta</b> etiqueta.
     *
     * <p>Solo las de aca; para lo que este vigente incluyendo lo heredado, ver
     * {@link #getNamespaceContext()}.
     *
     * @return un iterador de {@link Namespace}; vacio si no hay, nunca null
     */
    Iterator<Namespace> getNamespaces();

    /**
     * Un atributo por su nombre.
     *
     * <p>Como {@link QName#equals} ignora el prefijo, el nombre que se pase se puede construir con
     * cualquier prefijo o sin ninguno: lo que se compara es el espacio de nombres y el nombre
     * local. Para buscar un atributo sin calificar hay que pasar el espacio de nombres vacio, no el
     * del elemento.
     *
     * @param name el nombre buscado
     * @return el atributo, o null si el elemento no lo tiene
     */
    Attribute getAttributeByName(QName name);

    /**
     * El alcance de espacios de nombres vigente en este elemento, incluyendo el de los ancestros.
     *
     * @return el contexto; nunca null
     */
    NamespaceContext getNamespaceContext();

    /**
     * El URI asociado a un prefijo en este elemento.
     *
     * <p>Atajo de {@code getNamespaceContext().getNamespaceURI(prefix)}.
     *
     * @param prefix el prefijo; la cadena vacia para el espacio de nombres por omision
     * @return el URI, o null si el prefijo no esta declarado
     */
    String getNamespaceURI(String prefix);
}
