package com.sun.security.jgss;

import java.security.BasicPermission;

/**
 * The permission to call {@link ExtendedGSSContext#inquireSecContext}.
 *
 * <p>The permission's name is the {@link InquireType} that is authorized, and it admits a
 * wildcard: for example {@code "KRB5_GET_SESSION_KEY"} for a single one, or {@code "*"} for
 * them all.
 *
 * <p>It exists because `inquireSecContext` hands over material the context normally keeps --the
 * session key, among other things. A program that can read the session key can make messages
 * that look like the other end's; that is why the query is controlled apart from the use of the
 * context.
 */
public final class InquireSecContextPermission extends BasicPermission {

    private static final long serialVersionUID = -7131173349668647297L;

    /**
     * A permission for that {@link InquireType}, or for them all with {@code "*"}.
     *
     * @param name the constant's name, with a wildcard if it is wanted
     */
    public InquireSecContextPermission(String name) {
        super(name);
    }
}
