package java.rmi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * KajiLibrary's java.rmi.Naming -- la libreta de direcciones de RMI.
 *
 * <p>Cinco metodos estaticos sobre un registro remoto. Un servidor se anota con {@link #bind} o
 * {@link #rebind}, un cliente lo encuentra con {@link #lookup}.
 *
 * <h2>La forma del nombre</h2>
 *
 * <p>{@code rmi://maquina:puerto/nombre}, donde todo salvo el nombre se puede omitir: sin maquina es
 * la local, sin puerto es el 1099. El esquema, si esta, tiene que ser {@code rmi}.
 *
 * <p>Un nombre vacio no es un error: significa el registro mismo.
 *
 * <h2>Solo se modifica desde la misma maquina</h2>
 *
 * <p>{@link #bind}, {@link #rebind} y {@link #unbind} solo funcionan si el registro esta en la misma
 * maquina que quien llama; si no, {@link AccessException}. Buscar y listar si se puede de afuera.
 *
 * <p>Es la unica proteccion que tiene un registro, que por lo demas no autentica a nadie. Cualquiera
 * que llegue al puerto puede ver todo lo que hay anotado.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no tiene transporte RMI: hablar con un registro pide el protocolo JRMP entero
 * --talones, serializacion con anotacion de ubicacion, recoleccion distribuida-- y nada de eso esta.
 *
 * <p>El <b>analisis del nombre</b> si esta implementado, y es la parte que un programa nota primero:
 * un nombre mal formado lanza {@link MalformedURLException} igual que en el JDK, con la misma
 * distincion entre esquema invalido, URL no jerarquica y autoridad invalida. Despues de eso, las cinco
 * operaciones lanzan {@link ConnectException}, que es una {@link RemoteException} y es lo que ya
 * declaran.
 */
public final class Naming {

    /** El puerto de siempre de un registro RMI. */
    private static final int REGISTRY_PORT = 1099;

    /** No se instancia. */
    private Naming() {
    }

    /**
     * Busca ese nombre.
     *
     * @throws NotBoundException si no esta anotado
     * @throws MalformedURLException si el nombre no tiene la forma esperada
     * @throws RemoteException si no se pudo llegar al registro
     */
    public static Remote lookup(String name)
        throws NotBoundException, MalformedURLException, RemoteException {
        ParsedName parsed = parse(name);
        throw noTransport(parsed);
    }

    /**
     * Anota un objeto con ese nombre, sin pisar.
     *
     * @throws AlreadyBoundException si el nombre ya estaba
     * @throws MalformedURLException si el nombre no tiene la forma esperada
     * @throws RemoteException si no se pudo llegar al registro
     */
    public static void bind(String name, Remote obj)
        throws AlreadyBoundException, MalformedURLException, RemoteException {
        ParsedName parsed = parse(name);
        throw noTransport(parsed);
    }

    /**
     * Borra esa anotacion.
     *
     * @throws NotBoundException si no estaba
     * @throws MalformedURLException si el nombre no tiene la forma esperada
     * @throws RemoteException si no se pudo llegar al registro
     */
    public static void unbind(String name)
        throws RemoteException, NotBoundException, MalformedURLException {
        ParsedName parsed = parse(name);
        throw noTransport(parsed);
    }

    /**
     * Anota, pisando lo que hubiera.
     *
     * @throws MalformedURLException si el nombre no tiene la forma esperada
     * @throws RemoteException si no se pudo llegar al registro
     */
    public static void rebind(String name, Remote obj)
        throws RemoteException, MalformedURLException {
        ParsedName parsed = parse(name);
        throw noTransport(parsed);
    }

    /**
     * Todo lo anotado en ese registro.
     *
     * @param name la direccion del registro; el nombre se ignora
     * @throws MalformedURLException si el nombre no tiene la forma esperada
     * @throws RemoteException si no se pudo llegar al registro
     */
    public static String[] list(String name) throws RemoteException, MalformedURLException {
        ParsedName parsed = parse(name);
        throw noTransport(parsed);
    }

    /** El fallo declarado que comparten los cinco. Ver la nota de la clase. */
    private static ConnectException noTransport(ParsedName parsed) {
        return new ConnectException("Connection refused to host: " + parsed.host
            + "; no RMI transport in this library");
    }

    /**
     * Analiza {@code rmi://maquina:puerto/nombre}.
     *
     * @throws MalformedURLException si el esquema no es {@code rmi}, si la URL no es jerarquica, o si
     *     la autoridad no es una maquina y un puerto
     * @throws NullPointerException si es null
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
            // Una autoridad que arranca con dos puntos --"//:1099/x"-- es puerto sin maquina, y ahi
            // la maquina es la local. Sin esto, el puerto se perderia.
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

    /** Las tres partes de un nombre ya analizado. */
    private static final class ParsedName {

        /** La maquina, o vacio para la local. */
        final String host;

        /** El puerto, ya con el 1099 por omision puesto. */
        final int port;

        /** El nombre, o null si se pidio el registro mismo. */
        final String name;

        ParsedName(String host, int port, String name) {
            this.host = host;
            this.port = port;
            this.name = name;
        }
    }
}
