package com.sun.tools.attach;

import java.security.BasicPermission;

/**
 * El permiso para adjuntarse a otra VM.
 *
 * <p>Adjuntarse es tan poderoso como se puede ser: quien lo logra puede cargar un agente arbitrario
 * en el proceso destino, o sea ejecutar cualquier codigo con sus permisos. De ahi que sea una
 * accion con permiso propio.
 *
 * <p>El unico nombre definido es {@code "attachVirtualMachine"}. Extiende {@link BasicPermission},
 * asi que {@code "*"} tambien lo da. No tiene acciones; el segundo constructor las acepta y las
 * ignora, y existe solo porque el mecanismo de permisos construye por reflexion con dos argumentos.
 */
public final class AttachPermission extends BasicPermission {

    private static final long serialVersionUID = -4619447790611060661L;

    /** Un permiso con ese nombre. */
    public AttachPermission(String name) {
        super(name);
    }

    /** Igual; {@code actions} se ignora. */
    public AttachPermission(String name, String actions) {
        super(name, actions);
    }
}
