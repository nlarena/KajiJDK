package java.security;

// An entity: a person, a machine, a role. What a security system can name.
//
// It is the most used abstraction of `java.security` outside the package itself —`java.net`,
// `java.nio.file.attribute`, `javax.net.ssl` and `java.security.cert` cite it— and it is as small
// as it looks: a name, plus the equality in order to be able to compare them.
//
public interface Principal {

    // Whether this principal is equal to the given object.
    boolean equals(Object another);

    String toString();

    int hashCode();

    // The name of this principal.
    String getName();

    // Whether this principal is one of those of the given `Subject`.
    //
    // The default is the small answer: it looks for **this same** principal among those of the
    // Subject. Whoever wants the big answer —a role that takes in others, a group that contains
    // members— has to override it, and that is precisely the reason the method exists instead of
    // everybody doing `subject.getPrincipals().contains(p)`.
    //
    // A null Subject gives false and does not blow up: "nobody" never implies anybody.
    default boolean implies(javax.security.auth.Subject subject) {
        if (subject == null) {
            return false;
        }
        return subject.getPrincipals().contains(this);
    }
}
