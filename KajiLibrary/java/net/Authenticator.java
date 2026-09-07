package java.net;

// The place where an application leaves its credentials for when someone asks for them.
//
// The design is the other way round from what one would expect, and for a good reason: the
// application does **not** hand over credentials, it hands over an object that will be asked for
// them. That way whoever needs to authenticate --historically `HttpURLConnection`-- does not have to
// receive them as a parameter through ten layers of unrelated API, and the application decides on the
// spot whether to give them, to open a dialog, or not to answer.
//
// The `getRequesting*` methods are `protected` because they are **for the subclass**: when it is
// asked, the authenticator consults them to learn who is asking and decide. That they are instance
// fields mutated before each call, instead of method parameters, is an old JDK decision and is the
// reason for all the `synchronized` here: two threads asking at once would trample the fields.
//
// ===========================================================================================
// WHY THIS CLASS IS HONEST WITH NO NETWORK
// ===========================================================================================
//
// An `Authenticator` connects to nothing. It is a registry with a callback: `setDefault` stores,
// `requestPasswordAuthentication` consults. With nobody registered it returns null, which is exactly
// what the JDK does. Everything this API promises can be fulfilled here a hundred per cent -- what
// KajiJDK lacks is a **client** to use it, and that is not part of this contract.
//
// Nothing omitted.
public abstract class Authenticator {

    /** Who is asking for authentication: the destination server, or a proxy on the way. */
    public enum RequestorType {

        /** A proxy. */
        PROXY,

        /** The server one was trying to reach. */
        SERVER;
    }

    private static volatile Authenticator theAuthenticator;

    private String requestingHost;
    private InetAddress requestingSite;
    private int requestingPort;
    private String requestingProtocol;
    private String requestingPrompt;
    private String requestingScheme;
    private URL requestingURL;
    private RequestorType requestingAuthType;

    public Authenticator() {
    }

    private void reset() {
        this.requestingHost = null;
        this.requestingSite = null;
        this.requestingPort = -1;
        this.requestingProtocol = null;
        this.requestingPrompt = null;
        this.requestingScheme = null;
        this.requestingURL = null;
        this.requestingAuthType = RequestorType.SERVER;
    }

    /** Installs the whole VM's authenticator. */
    public static synchronized void setDefault(Authenticator a) {
        theAuthenticator = a;
    }

    /** The installed authenticator, or null if there is none. */
    public static Authenticator getDefault() {
        return theAuthenticator;
    }

    /** Asks the installed authenticator for credentials; null if there is none or it gives none. */
    public static PasswordAuthentication requestPasswordAuthentication(
            InetAddress addr, int port, String protocol, String prompt, String scheme) {
        return requestPasswordAuthentication(
                theAuthenticator, null, addr, port, protocol, prompt, scheme, null,
                RequestorType.SERVER);
    }

    /** Like the previous one, but identifying the host by name as well as by address. */
    public static PasswordAuthentication requestPasswordAuthentication(
            String host, InetAddress addr, int port, String protocol, String prompt,
            String scheme) {
        return requestPasswordAuthentication(
                theAuthenticator, host, addr, port, protocol, prompt, scheme, null,
                RequestorType.SERVER);
    }

    /** Like the previous one, plus the URL that triggered the request and who is asking. */
    public static PasswordAuthentication requestPasswordAuthentication(
            String host, InetAddress addr, int port, String protocol, String prompt,
            String scheme, URL url, RequestorType reqType) {
        return requestPasswordAuthentication(
                theAuthenticator, host, addr, port, protocol, prompt, scheme, url, reqType);
    }

    /**
     * Like the previous one, but asking a specific authenticator instead of the installed one.
     *
     * <p>It exists so that a library can have its own authenticator without trampling the global one
     * of the application using it.
     */
    public static PasswordAuthentication requestPasswordAuthentication(
            Authenticator authenticator, String host, InetAddress addr, int port, String protocol,
            String prompt, String scheme, URL url, RequestorType reqType) {
        Authenticator a = (authenticator == null) ? theAuthenticator : authenticator;
        if (a == null) {
            return null;
        }
        synchronized (a) {
            a.reset();
            a.requestingHost = host;
            a.requestingSite = addr;
            a.requestingPort = port;
            a.requestingProtocol = protocol;
            a.requestingPrompt = prompt;
            a.requestingScheme = scheme;
            a.requestingURL = url;
            a.requestingAuthType = reqType;
            return a.getPasswordAuthentication();
        }
    }

    /** Asks **this** instance for credentials, without going through the installed authenticator. */
    public PasswordAuthentication requestPasswordAuthenticationInstance(
            String host, InetAddress addr, int port, String protocol, String prompt,
            String scheme, URL url, RequestorType reqType) {
        synchronized (this) {
            this.reset();
            this.requestingHost = host;
            this.requestingSite = addr;
            this.requestingPort = port;
            this.requestingProtocol = protocol;
            this.requestingPrompt = prompt;
            this.requestingScheme = scheme;
            this.requestingURL = url;
            this.requestingAuthType = reqType;
            return this.getPasswordAuthentication();
        }
    }

    /** The name of the host asking, or null if only the address was given. */
    protected final String getRequestingHost() {
        return this.requestingHost;
    }

    /** The address of the one asking, or null if only the name was given. */
    protected final InetAddress getRequestingSite() {
        return this.requestingSite;
    }

    protected final int getRequestingPort() {
        return this.requestingPort;
    }

    /** The connection's protocol ("http", "ftp"...). */
    protected final String getRequestingProtocol() {
        return this.requestingProtocol;
    }

    /** The text the server sent to be shown to the user (HTTP Basic's "realm"). */
    protected final String getRequestingPrompt() {
        return this.requestingPrompt;
    }

    /** The authentication scheme ("basic", "digest"...). */
    protected final String getRequestingScheme() {
        return this.requestingScheme;
    }

    /**
     * What the subclass overrides in order to hand over credentials. Returning null means "I am not
     * giving them", and it is the default answer: an authenticator that overrides nothing
     * authenticates nothing.
     */
    protected PasswordAuthentication getPasswordAuthentication() {
        return null;
    }

    /** The URL that triggered the request, or null if none was given. */
    protected URL getRequestingURL() {
        return this.requestingURL;
    }

    /** Whether the one asking is the server or a proxy. */
    protected RequestorType getRequestorType() {
        return this.requestingAuthType;
    }
}
