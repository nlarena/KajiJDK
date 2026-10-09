package java.rmi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * KajiLibrary's java.rmi.Naming -- RMI's address book.
 *
 * <p>Five static methods over a remote registry. A server records itself with {@link #bind} or
 * {@link #rebind}, a client finds it with {@link #lookup}.
 *
 * <h2>The shape of the name</h2>
 *
 * <p>{@code rmi://host:port/name}, where everything but the name may be omitted: with no host it is
 * the local one, with no port it is 1099. The scheme, if present, has to be {@code rmi}.
 *
 * <p>An empty name is not an error: it means the registry itself.
 *
 * <h2>Modified only from the same machine</h2>
 *
 * <p>{@link #bind}, {@link #rebind} and {@link #unbind} only work if the registry is on the same
 * machine as the caller; otherwise, {@link AccessException}. Looking up and listing do work from
 * outside.
 *
 * <p>It is the only protection a registry has, which otherwise authenticates nobody. Anyone who
 * reaches the port can see everything recorded there.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library has no RMI transport: talking to a registry takes the whole JRMP protocol
 * --stubs, serialisation with location annotation, distributed garbage collection-- and none of
 * that is here.
 *
 * <p><b>Name parsing</b> is implemented, and it is the part a program notices first: a malformed
 * name throws {@link MalformedURLException} as in the JDK, with the same distinction between an
 * invalid scheme and a non-hierarchical URL. This note used to list an invalid authority as a third
 * case; with this library's {@link URI} that case never fires, because {@code getHost()} is null
 * only when there is no authority at all and the constructor does not validate the authority
 * (checked in {@code java/net/URI.java}), so {@code rmi://host:abc/x} is accepted with port 1099.
 * After that, the five operations throw {@link ConnectException}, which is a
 * {@link RemoteException} and is what they already declare.
 */
public final class Naming {

    /** The usual port of an RMI registry. */
    private static final int REGISTRY_PORT = 1099;

    /** It is not instantiated. */
    private Naming() {
    }

    /**
     * It looks up that name.
     *
     * @throws NotBoundException if it is not bound
     * @throws MalformedURLException if the name does not have the expected shape
     * @throws RemoteException if the registry could not be reached
     */
    public static Remote lookup(String name)
        throws NotBoundException, MalformedURLException, RemoteException {
        ParsedName parsed = parse(name);
        throw noTransport(parsed);
    }

    /**
     * It binds an object to that name, without overwriting.
     *
     * @throws AlreadyBoundException if the name was already bound
     * @throws MalformedURLException if the name does not have the expected shape
     * @throws RemoteException if the registry could not be reached
     */
    public static void bind(String name, Remote obj)
        throws AlreadyBoundException, MalformedURLException, RemoteException {
        ParsedName parsed = parse(name);
        throw noTransport(parsed);
    }

    /**
     * It removes that binding.
     *
     * @throws NotBoundException if it was not bound
     * @throws MalformedURLException if the name does not have the expected shape
     * @throws RemoteException if the registry could not be reached
     */
    public static void unbind(String name)
        throws RemoteException, NotBoundException, MalformedURLException {
        ParsedName parsed = parse(name);
        throw noTransport(parsed);
    }

    /**
     * It binds, overwriting whatever was there.
     *
     * @throws MalformedURLException if the name does not have the expected shape
     * @throws RemoteException if the registry could not be reached
     */
    public static void rebind(String name, Remote obj)
        throws RemoteException, MalformedURLException {
        ParsedName parsed = parse(name);
        throw noTransport(parsed);
    }

    /**
     * Everything bound in that registry.
     *
     * @param name the registry's address; the name part is ignored
     * @throws MalformedURLException if the name does not have the expected shape
     * @throws RemoteException if the registry could not be reached
     */
    public static String[] list(String name) throws RemoteException, MalformedURLException {
        ParsedName parsed = parse(name);
        throw noTransport(parsed);
    }

    /** The declared failure the five share. See the class note. */
    private static ConnectException noTransport(ParsedName parsed) {
        return new ConnectException("Connection refused to host: " + parsed.host
            + "; no RMI transport in this library");
    }

    /**
     * It parses {@code rmi://host:port/name}.
     *
     * @throws MalformedURLException if the scheme is not {@code rmi} or if the URL is not
     *     hierarchical. This used to add "or if the authority is not a host and a port"; the
     *     authority checks below are unreachable with this library's URI (see the note there), so
     *     {@code //host:abc/x} is accepted with port 1099
     * @throws NullPointerException if it is null
     */
    private static ParsedName parse(String str) throws MalformedURLException {
        if (str == null) {
            throw new NullPointerException();
        }
        URI uri;
        try {
            uri = new URI(str);
        } catch (URISyntaxException e) {
            throw new MalformedURLException("invalid URL String: " + str);
        }
        if (uri.isOpaque()) {
            throw new MalformedURLException("not a hierarchical URL: " + str);
        }
        if (uri.getFragment() != null) {
            throw new MalformedURLException("invalid character, '#', in URL name: " + str);
        }
        if (uri.getQuery() != null) {
            throw new MalformedURLException("invalid character, '?', in URL name: " + str);
        }
        if (uri.getUserInfo() != null) {
            throw new MalformedURLException("invalid character, '@', in URL host: " + str);
        }
        String scheme = uri.getScheme();
        if (scheme != null && !scheme.equals("rmi")) {
            throw new MalformedURLException("invalid URL scheme: " + str);
        }
        String name = uri.getPath();
        if (name != null) {
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            if (name.length() == 0) {
                name = null;
            }
        }
        String host = uri.getHost();
        int port = uri.getPort();
        if (host == null) {
            // An authority that starts with a colon --"//:1099/x"-- is a port with no host, and
            // there the host is the local one. This note used to say that without this the port
            // would be lost; with this library's URI it is not: for ":1099" getHost() returns ""
            // and getPort() 1099, and getHost() is null only when the authority itself is null, so
            // this branch only ever sees a null authority and the two checks inside never fire
            // (checked in java/net/URI.java).
            String authority = uri.getAuthority();
            if (authority != null && authority.startsWith(":")) {
                try {
                    port = Integer.parseInt(authority.substring(1));
                } catch (NumberFormatException e) {
                    throw new MalformedURLException("invalid authority: " + str);
                }
            } else if (authority != null && authority.length() > 0) {
                throw new MalformedURLException("invalid authority: " + str);
            }
            host = "";
        }
        if (port == -1) {
            port = REGISTRY_PORT;
        }
        return new ParsedName(host, port, name);
    }

    /** The three parts of an already parsed name. */
    private static final class ParsedName {

        /** The host, or empty for the local one. */
        final String host;

        /** The port, with the default 1099 already filled in. */
        final int port;

        /** The name, or null if the registry itself was asked for. */
        final String name;

        ParsedName(String host, int port, String name) {
            this.host = host;
            this.port = port;
            this.name = name;
        }
    }
}
