package jdk.net;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;

/**
 * Who is on the other side of a Unix domain socket: their user and their group.
 *
 * <h2>Why this only exists for Unix domain sockets</h2>
 *
 * <p>Over TCP, the identity of the peer cannot be found out: the only thing there is is an address,
 * and an address does not say who runs the process that uses it. A Unix domain socket lives inside a
 * single machine, so the kernel <strong>does</strong> know which user opened the other end and can
 * tell — which turns these sockets into a channel where one can authorise without credentials of
 * one's own.
 *
 * <p>It is read with the option {@link ExtendedSocketOptions#SO_PEERCRED}.
 *
 * <p>It is a {@code record} and not a class with getters because it is exactly that: two values,
 * with no behaviour, comparable by contents.
 *
 * @param user the user who opened the other end
 * @param group their group
 */
public record UnixDomainPrincipal(UserPrincipal user, GroupPrincipal group) {

    /**
     * @throws NullPointerException if either is {@code null} — a half principal identifies nobody, and
     *     letting it through only changes where it blows up
     */
    // The JDK writes it in the COMPACT form (`public UnixDomainPrincipal {`), which our parser does
    // not accept yet: finding #403. The complete canonical form is equivalent —the compiler only adds
    // the assignments that are written here— and it is what that finding records as the way round.
    public UnixDomainPrincipal(UserPrincipal user, GroupPrincipal group) {
        if (user == null) {
            throw new NullPointerException("user");
        }
        if (group == null) {
            throw new NullPointerException("group");
        }
        this.user = user;
        this.group = group;
    }
}
