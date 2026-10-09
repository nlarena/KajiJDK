package javax.naming;

/**
 * The one that knows how to turn a string into a `Name` **with a namespace's syntax**.
 *
 * <p>It exists because the syntax belongs not to the caller but to the provider: to parse an LDAP
 * name you need to know it separates with a comma, quotes with double quotes and orders right to
 * left. Instead of exposing those properties, `Context.getNameParser()` returns an object that
 * already has them inside, and the caller just passes it the string.
 *
 * <p>Two different contexts may return the **same** parser, and there the contract says something
 * useful: if `p1.equals(p2)`, both names live in the same namespace and can be compared with each
 * other. It is the only way to know that without asking the server.
 */
public interface NameParser {

    Name parse(String name) throws NamingException;
}
