package com.sun.security.jgss;

import java.security.BasicPermission;

/**
 * El permiso para llamar a {@link ExtendedGSSContext#inquireSecContext}.
 *
 * <p>El nombre del permiso es el {@link InquireType} que se autoriza, y admite comodin: por ejemplo
 * {@code "KRB5_GET_SESSION_KEY"} para uno solo, o {@code "*"} para todos.
 *
 * <p>Existe porque `inquireSecContext` entrega material que el contexto normalmente guarda --la
 * clave de sesion, entre otras cosas. Un programa que puede leer la clave de sesion puede fabricar
 * mensajes que parezcan del otro extremo; por eso la consulta se controla aparte del uso del
 * contexto.
 */
public final class InquireSecContextPermission extends BasicPermission {

    private static final long serialVersionUID = -7131173349668647297L;

    /**
     * Un permiso para ese {@link InquireType}, o para todos con {@code "*"}.
     *
     * @param name el nombre de la constante, con comodin si se quiere
     */
    public InquireSecContextPermission(String name) {
        super(name);
    }
}
