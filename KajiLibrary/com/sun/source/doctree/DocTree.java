package com.sun.source.doctree;

/**
 * Cualquier nodo del arbol de un comentario de documentacion.
 *
 * <h2>Que es este paquete</h2>
 *
 * <p>Un comentario de javadoc no es texto: tiene estructura —tags, HTML, referencias a codigo— y una
 * herramienta que quiera hacer algo con el necesita esa estructura, no la cadena. Este paquete es el
 * arbol de sintaxis de esa estructura, hermano del de {@code com.sun.source.tree} para el codigo.
 *
 * <h2>Las dos formas de recorrerlo</h2>
 *
 * <p>{@link #getKind} y {@link #accept} son la misma pregunta contestada de dos maneras, y estan las
 * dos a proposito. El {@link Kind} sirve para una decision suelta —"¿esto es un `@param`?"— sin
 * escribir un visitante entero. El {@link DocTreeVisitor} sirve cuando hay que atender a todos: el
 * compilador avisa si se agrega un tipo de nodo y falta el metodo, mientras que un {@code switch}
 * sobre el {@code Kind} se queda callado.
 *
 * <h2>La trampa: mas nodos que tipos</h2>
 *
 * <p>{@link Kind} tiene mas constantes que interfaces hay en el paquete, y no es un descuido.
 * `{@code @throws}` y `{@code @exception}` son el mismo {@link ThrowsTree} con distinto
 * {@code tagName}; lo mismo `{@code {@link}}` y `{@code {@linkplain}}`, y `{@code {@code}}` con
 * `{@code {@literal}}`. Preguntar por el tipo Java no alcanza para distinguirlos — hay que mirar el
 * nombre del tag.
 */
public interface DocTree {

    /**
     * Que clase de nodo es este.
     *
     * <p>Cada constante lleva el {@code tagName} con el que se escribe, o {@code null} para los
     * nodos que no son un tag: texto, HTML, entidades, el comentario entero.
     */
    enum Kind {

        /** Un atributo de una etiqueta HTML. */
        ATTRIBUTE(null),
        /** `@author`. */
        AUTHOR("author"),
        /** `{@code}` — el mismo {@link LiteralTree} que {@link #LITERAL}. */
        CODE("code"),
        /** Un comentario HTML. */
        COMMENT(null),
        /** `@deprecated`. */
        DEPRECATED("deprecated"),
        /** El comentario entero: la raiz. */
        DOC_COMMENT(null),
        /** `{@docRoot}`. */
        DOC_ROOT("docRoot"),
        /** Un `<!DOCTYPE>`. */
        DOC_TYPE(null),
        /** Una etiqueta HTML de cierre. */
        END_ELEMENT(null),
        /** Una entidad HTML. */
        ENTITY(null),
        /** Algo que no se pudo parsear. */
        ERRONEOUS(null),
        /** Un escape de Markdown. */
        ESCAPE(null),
        /** `@exception` — el mismo {@link ThrowsTree} que {@link #THROWS}. */
        EXCEPTION("exception"),
        /** `@hidden`. */
        HIDDEN("hidden"),
        /** Un identificador de Java dentro de un tag. */
        IDENTIFIER(null),
        /** `{@index}`. */
        INDEX("index"),
        /** `{@inheritDoc}`. */
        INHERIT_DOC("inheritDoc"),
        /** `{@link}`. */
        LINK("link"),
        /** `{@linkplain}` — el mismo {@link LinkTree} que {@link #LINK}. */
        LINK_PLAIN("linkplain"),
        /** `{@literal}`. */
        LITERAL("literal"),
        /** Contenido Markdown sin interpretar. */
        MARKDOWN(null),
        /** `@param`. */
        PARAM("param"),
        /** `@provides`. */
        PROVIDES("provides"),
        /** Una referencia a un elemento de Java. */
        REFERENCE(null),
        /** `@return`, en cualquiera de sus dos formas. */
        RETURN("return"),
        /** `@see`. */
        SEE("see"),
        /** `@serial`. */
        SERIAL("serial"),
        /** `@serialData`. */
        SERIAL_DATA("serialData"),
        /** `@serialField`. */
        SERIAL_FIELD("serialField"),
        /** `@since`. */
        SINCE("since"),
        /** `{@snippet}`. */
        SNIPPET("snippet"),
        /** `@spec`. */
        SPEC("spec"),
        /** Una etiqueta HTML de apertura. */
        START_ELEMENT(null),
        /** `{@systemProperty}`. */
        SYSTEM_PROPERTY("systemProperty"),
        /** `{@summary}`. */
        SUMMARY("summary"),
        /** Texto plano. */
        TEXT(null),
        /** `@throws`. */
        THROWS("throws"),
        /** Un tag de bloque que este arbol no conoce. */
        UNKNOWN_BLOCK_TAG(null),
        /** Un tag en linea que este arbol no conoce. */
        UNKNOWN_INLINE_TAG(null),
        /** `@uses`. */
        USES("uses"),
        /** `{@value}`. */
        VALUE("value"),
        /** `@version`. */
        VERSION("version"),
        /**
         * Una implementacion propia que no es ninguno de los anteriores.
         *
         * <p>Existe por la misma razon que {@link #UNKNOWN_BLOCK_TAG}: este arbol es una interfaz
         * publica y alguien puede implementarla con nodos que el JDK no previo. Sin esta constante,
         * {@link #getKind} no tendria que devolver.
         */
        OTHER(null);

        /**
         * El nombre con el que se escribe el tag, o {@code null} si este nodo no es un tag.
         *
         * <p>Publico y final, no un getter, y asi es en el JDK: es un dato del enum, no un calculo.
         * Es ademas lo unico que separa a {@link #THROWS} de {@link #EXCEPTION}, que comparten
         * interfaz.
         */
        public final String tagName;

        Kind(String tagName) {
            this.tagName = tagName;
        }
    }

    /** Que clase de nodo es. */
    Kind getKind();

    /**
     * Le pasa este nodo al visitante.
     *
     * <p>Doble despacho: el nodo sabe cual es su tipo y el visitante sabe que hacer con cada uno;
     * ninguno de los dos sabe las dos cosas, y este metodo es donde se juntan.
     *
     * @param <R> lo que devuelve el visitante
     * @param <D> el dato que se le arrastra
     */
    <R, D> R accept(DocTreeVisitor<R, D> visitor, D data);
}
