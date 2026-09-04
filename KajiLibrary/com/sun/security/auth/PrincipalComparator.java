package com.sun.security.auth;

import javax.security.auth.Subject;

/**
 * Un principal que sabe decir si <em>implica</em> a un sujeto entero.
 *
 * <h2>Que quiere decir "implica"</h2>
 *
 * <p>Que una politica escrita para este principal alcanza a ese sujeto. Lo normal es que un sujeto
 * tenga varios principales —usuario, grupos, dominio— y una politica escrita para el grupo
 * {@code admin} alcance a todo el que lo tenga entre los suyos.
 *
 * <p>Sin esto, comparar seria buscar igualdad exacta contra cada principal del sujeto, y no habria
 * forma de expresar un principal que represente a un conjunto.
 *
 * @deprecated el mecanismo de politicas basado en {@code Subject} quedo en desuso junto con el
 *     gestor de seguridad.
 */
@Deprecated(since = "17", forRemoval = true)
public interface PrincipalComparator {

    /** Si una politica escrita para este principal alcanza a {@code subject}. */
    boolean implies(Subject subject);
}
