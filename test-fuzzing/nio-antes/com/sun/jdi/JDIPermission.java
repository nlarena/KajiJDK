package com.sun.jdi;

/**
 * The permission that guarded the access to JDI.
 *
 * <p>It is kept because it is public API, even though the {@code SecurityManager} has been
 * disabled since JDK 24 and the check no longer happens.
 *
 * @since 1.3
 */
public final class JDIPermission extends java.security.BasicPermission {

    private static final long serialVersionUID = -6988461416938786271L;

    /**
     * A permission with that name.
     *
     * @param name it has to be {@code "virtualMachineManager"}
     * @throws IllegalArgumentException if the name is another
     */
    public JDIPermission(String name) {
        super(name);
        if (!"virtualMachineManager".equals(name)) {
            throw new IllegalArgumentException("the only valid name is virtualMachineManager");
        }
    }

    /**
     * A permission with that name and those actions.
     *
     * @param name it has to be {@code "virtualMachineManager"}
     * @param actions it has to be {@code null} or empty; this permission has no actions
     * @throws IllegalArgumentException if the name is another or there are actions
     */
    public JDIPermission(String name, String actions) throws IllegalArgumentException {
        super(name, actions);
        if (!"virtualMachineManager".equals(name)) {
            throw new IllegalArgumentException("the only valid name is virtualMachineManager");
        }
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException("JDIPermission does not have actions");
        }
    }
}
