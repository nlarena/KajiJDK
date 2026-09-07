package com.sun.jdi;

/**
 * El permiso que guardaba el acceso a JDI.
 *
 * <p>Se conserva porque es API publica, aunque el {@code SecurityManager} este deshabilitado desde
 * JDK 24 y el chequeo ya no ocurra.
 *
 * @since 1.3
 */
public final class JDIPermission extends java.security.BasicPermission {

    private static final long serialVersionUID = -6988461416938786271L;

    /**
     * Un permiso con ese nombre.
     *
     * @param name tiene que ser {@code "virtualMachineManager"}
     * @throws IllegalArgumentException si el nombre es otro
     */
    public JDIPermission(String name) {
        super(name);
        if (!"virtualMachineManager".equals(name)) {
            throw new IllegalArgumentException("el unico nombre valido es virtualMachineManager");
        }
    }

    /**
     * Un permiso con ese nombre y esas acciones.
     *
     * @param name tiene que ser {@code "virtualMachineManager"}
     * @param actions tiene que ser {@code null} o vacio; este permiso no tiene acciones
     * @throws IllegalArgumentException si el nombre es otro o hay acciones
     */
    public JDIPermission(String name, String actions) throws IllegalArgumentException {
        super(name, actions);
        if (!"virtualMachineManager".equals(name)) {
            throw new IllegalArgumentException("el unico nombre valido es virtualMachineManager");
        }
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException("JDIPermission no tiene acciones");
        }
    }
}
