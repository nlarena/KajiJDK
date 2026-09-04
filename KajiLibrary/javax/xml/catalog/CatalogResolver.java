package javax.xml.catalog;

import java.io.InputStream;
import javax.xml.stream.XMLResolver;
import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * KajiLibrary's javax.xml.catalog.CatalogResolver -- enchufa un {@link Catalog} en las cuatro APIs de
 * XML.
 *
 * <p>Extiende las cuatro interfaces de resolucion que hay en la plataforma --SAX, StAX, transformacion
 * y DOM-- para que un mismo objeto sirva de resolutor en cualquiera de ellas. Es lo que hace que
 * configurar un catalogo sea una linea y no cuatro implementaciones.
 *
 * <h2>Que pasa cuando no hay coincidencia</h2>
 *
 * <p>Lo decide {@link NotFoundAction}, que sale de la caracteristica {@code RESOLVE}. Es la parte que
 * hay que entender antes de usar esto en produccion; ver ahi.
 *
 * <h2>Los dos {@code resolveEntity} de cuatro argumentos</h2>
 *
 * <p>Hay uno solo declarado, que devuelve {@link InputStream}; el otro que aparece en el bytecode es
 * el puente que el compilador genera porque {@link XMLResolver} lo declara devolviendo {@code Object}.
 * Es un detalle de compilacion, no dos metodos.
 */
public interface CatalogResolver
    extends EntityResolver, XMLResolver, URIResolver, LSResourceResolver {

    /**
     * Que hacer cuando el catalogo no tiene la entrada.
     *
     * <p>Las tres opciones son muy distintas y elegir mal se paga tarde:
     *
     * <ul>
     *   <li>{@link #STRICT} --el de omision-- lanza {@link CatalogException}. Es lo que hay que usar
     *       en un despliegue cerrado: si el catalogo no cubre algo, se quiere saber;
     *   <li>{@link #CONTINUE} devuelve null, que en todas estas APIs significa "resolvelo vos como
     *       siempre". O sea: <b>sale a la red</b>. Es lo que se quiere cuando el catalogo es una
     *       cache y no una restriccion;
     *   <li>{@link #IGNORE} devuelve algo vacio. El analizador sigue como si el recurso existiera y
     *       estuviera en blanco -- util para saltear una DTD que solo declara entidades que no se
     *       usan, y peligroso si esa DTD definia valores por omision.
     * </ul>
     */
    enum NotFoundAction {

        /** Devolver null y dejar que la API resuelva sola. Ver la nota. */
        CONTINUE("continue"),

        /** Devolver algo vacio. Ver la nota. */
        IGNORE("ignore"),

        /** Lanzar {@link CatalogException}. Es el de omision. */
        STRICT("strict");

        /** El nombre que usa la caracteristica {@code RESOLVE}. */
        private final String literal;

        NotFoundAction(String literal) {
            this.literal = literal;
        }

        /** El nombre en minusculas, no el de la constante. */
        @Override
        public String toString() {
            return this.literal;
        }

        /**
         * La accion de ese nombre.
         *
         * @throws IllegalArgumentException si no es ninguna; distingue mayusculas
         */
        public static NotFoundAction getType(String literal) {
            NotFoundAction[] all = values();
            int i = 0;
            while (i < all.length) {
                if (all[i].literal.equals(literal)) {
                    return all[i];
                }
                i = i + 1;
            }
            throw CatalogMessages.invalidArgument(literal, "RESOLVE");
        }
    }

    /**
     * La resolucion de SAX.
     *
     * @return la fuente, o null; ver {@link NotFoundAction}
     * @throws CatalogException en modo estricto sin coincidencia
     */
    InputSource resolveEntity(String publicId, String systemId);

    /**
     * La resolucion de las transformaciones.
     *
     * @return la fuente, o null
     * @throws CatalogException en modo estricto sin coincidencia
     */
    Source resolve(String href, String base);

    /**
     * La resolucion de StAX.
     *
     * <p>Devuelve {@link InputStream} y no {@code Object} como {@link XMLResolver}: es un
     * estrechamiento del tipo de retorno, permitido y mas util.
     *
     * @return el flujo, o null
     * @throws CatalogException en modo estricto sin coincidencia
     */
    InputStream resolveEntity(String publicId, String systemId, String baseURI, String namespace);

    /**
     * La resolucion de DOM.
     *
     * @return la entrada, o null
     * @throws CatalogException en modo estricto sin coincidencia
     */
    LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId,
                            String baseURI);
}
