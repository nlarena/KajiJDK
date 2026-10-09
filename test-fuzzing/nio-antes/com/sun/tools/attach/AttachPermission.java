package com.sun.tools.attach;

import java.security.BasicPermission;

/**
 * The permission to attach to another VM.
 *
 * <p>Attaching is as powerful as one can be: whoever manages it may load an arbitrary agent into
 * the target process, that is, execute any code with its permissions. Hence it is an action with
 * a permission of its own.
 *
 * <p>The only defined name is {@code "attachVirtualMachine"}. It extends
 * {@link BasicPermission}, so {@code "*"} gives it too. It has no actions; the second
 * constructor accepts them and ignores them, and exists only because the permission mechanism
 * builds by reflection with two arguments.
 */
public final class AttachPermission extends BasicPermission {

    private static final long serialVersionUID = -4619447790611060661L;

    /** A permission with that name. */
    public AttachPermission(String name) {
        super(name);
    }

    /** The same; {@code actions} is ignored. */
    public AttachPermission(String name, String actions) {
        super(name, actions);
    }
}
