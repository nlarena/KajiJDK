package com.sun.source.doctree;

import java.util.List;

import javax.lang.model.element.Name;

/**
 * Un atributo de una etiqueta HTML dentro del comentario, como el {@code href} de un {@code <a>}.
 *
 * <p>El valor es una <strong>lista de nodos</strong> y no una cadena porque adentro puede haber
 * cosas que el arbol tiene que representar: una entidad HTML, un {@code {@docRoot}}. Aplanarlo a
 * texto perderia esa estructura justo donde hace falta — un {@code href} que empieza con
 * {@code {@docRoot}} es lo que hace que un enlace funcione desde cualquier profundidad.
 */
public interface AttributeTree extends DocTree {

    /**
     * Como estaba escrito el valor del atributo.
     *
     * <p>Se conserva en vez de normalizarse porque javadoc reemite el HTML, y reemitir
     * {@code width=5} como {@code width="5"} cambiaria lo que el autor escribio. Distinguir las
     * comillas simples de las dobles importa por lo mismo.
     */
    enum ValueKind {

        /** Sin valor: el atributo esta solo, como el {@code checked} de un {@code <input>}. */
        EMPTY,
        /** Con valor y sin comillas: {@code width=5}. */
        UNQUOTED,
        /** Entre comillas simples. */
        SINGLE,
        /** Entre comillas dobles. */
        DOUBLE
    }

    /** El nombre del atributo. */
    Name getName();

    /** Como venia escrito el valor; ver {@link ValueKind}. */
    ValueKind getValueKind();

    /** El valor, o {@code null} si el {@link ValueKind} es {@link ValueKind#EMPTY}. */
    List<? extends DocTree> getValue();
}
