package org.xml.sax.ext;

import java.io.IOException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * KajiLibrary's org.xml.sax.ext.EntityResolver2 -- el resolvedor de SAX1 arreglado en los dos
 * lugares donde no alcanzaba.
 *
 * <p>El primero: `EntityResolver.resolveEntity(publicId, systemId)` recibe un identificador de
 * sistema que el parser **ya resolvio** contra la base, asi que el resolvedor ve una URI absoluta y
 * no puede saber que decia el documento ni contra que se resolvio. Aca llegan los cuatro datos por
 * separado --nombre, publicId, baseURI y el systemId tal como estaba escrito-- y con eso sí se
 * puede decidir.
 *
 * <p>El segundo, y es el que motivo la extension: **no habia forma de darle una DTD a un documento
 * que no la pide.** Un documento sin `&lt;!DOCTYPE&gt;` no genera ninguna llamada de resolucion, y
 * sin DTD no hay valores por omision de atributos ni entidades declaradas. {@link
 * #getExternalSubset} se llama para todo documento que no tenga subconjunto externo propio, justo
 * para poder inyectarle uno.
 *
 * <p>Se instala en el mismo lugar que el de siempre, con `setEntityResolver`; el parser hace
 * `instanceof` y usa los metodos nuevos si estan. Que los use se ve en la feature
 * `http://xml.org/sax/features/use-entity-resolver2`, que es de lectura y escritura: poniendola en
 * `false` se le pide al parser que trate el objeto como un `EntityResolver` viejo aunque implemente
 * esta interfaz.
 *
 * <p><strong>Devolver `null` desde cualquiera de los dos metodos no es un stub</strong>, es la
 * respuesta que el contrato define para "no tengo nada que sustituir, abrilo vos por el
 * identificador de sistema". Es lo que hace {@link DefaultHandler2}.
 *
 * <p>Sobre el orden: `getExternalSubset` se llama **antes** de `LexicalHandler.startDTD`, y lo que
 * devuelva se analiza como si fuera el subconjunto externo declarado. Si el documento ya tiene uno,
 * este metodo no se llama y no hay forma de agregar un segundo.
 */
public interface EntityResolver2 extends EntityResolver {

    /**
     * Un subconjunto externo para un documento que no declara ninguno, o `null` para dejarlo sin
     * DTD. `name` es el nombre del elemento raiz, que es el unico dato con el que se puede elegir
     * una DTD para un documento que no la nombro.
     */
    InputSource getExternalSubset(String name, String baseURI)
            throws SAXException, IOException;

    /**
     * `name` es `[dtd]` para el subconjunto externo, o el nombre de la entidad --con `%` adelante
     * si es de parametro--. `systemId` viene **sin** resolver, tal como aparece en el documento;
     * `baseURI` es contra que habria que resolverlo, y puede ser `null` cuando el parser no sabe de
     * donde venia la entidad que lo contiene.
     */
    InputSource resolveEntity(String name, String publicId,
                              String baseURI, String systemId)
            throws SAXException, IOException;
}
