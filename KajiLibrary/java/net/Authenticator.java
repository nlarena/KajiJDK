package java.net;

// El lugar donde la aplicacion deja sus credenciales para cuando alguien se las pida.
//
// El diseno es al reves de lo que uno esperaria y por una buena razon: la aplicacion **no** entrega
// credenciales, entrega un objeto al que se le van a pedir. Asi el que necesita autenticarse
// --historicamente `HttpURLConnection`-- no tiene que recibirlas por parametro atravesando diez
// capas de API que no tienen nada que ver, y la aplicacion decide en el momento si las da, si abre
// un dialogo, o si no contesta.
//
// Los `getRequesting*` son `protected` porque son **para la subclase**: cuando le preguntan, el
// authenticator los consulta para saber quien pregunta y decidir. Que sean campos de instancia
// mutados antes de cada llamada, en vez de parametros del metodo, es una decision vieja del JDK y
// es la razon de todos los `synchronized` de aca: dos hilos preguntando a la vez se pisarian los
// campos.
//
// ===========================================================================================
// POR QUE ESTA CLASE ES HONESTA SIN RED
// ===========================================================================================
//
// Un `Authenticator` no se conecta a nada. Es un registro con un callback: `setDefault` guarda,
// `requestPasswordAuthentication` consulta. Sin nadie registrado devuelve null, que es exactamente
// lo que hace el JDK. Todo lo que promete esta API se puede cumplir aca al cien por ciento -- lo que
// falta en KajiJDK es un **cliente** que la use, y eso no es parte de este contrato.
//
// Nada omitido.
public abstract class Authenticator {

    /** Quien esta pidiendo autenticacion: el servidor de destino, o un proxy en el camino. */
    public enum RequestorType {

        /** Un proxy. */
        PROXY,

        /** El servidor al que se queria llegar. */
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

    /** Instala el authenticator de toda la VM. */
    public static synchronized void setDefault(Authenticator a) {
        theAuthenticator = a;
    }

    /** El authenticator instalado, o null si no hay ninguno. */
    public static Authenticator getDefault() {
        return theAuthenticator;
    }

    /** Le pide credenciales al authenticator instalado; null si no hay ninguno o si no las da. */
    public static PasswordAuthentication requestPasswordAuthentication(
            InetAddress addr, int port, String protocol, String prompt, String scheme) {
        return requestPasswordAuthentication(
                theAuthenticator, null, addr, port, protocol, prompt, scheme, null,
                RequestorType.SERVER);
    }

    /** Como la anterior, pero identificando al host por nombre ademas de por direccion. */
    public static PasswordAuthentication requestPasswordAuthentication(
            String host, InetAddress addr, int port, String protocol, String prompt,
            String scheme) {
        return requestPasswordAuthentication(
                theAuthenticator, host, addr, port, protocol, prompt, scheme, null,
                RequestorType.SERVER);
    }

    /** Como la anterior, mas la URL que gatillo el pedido y quien lo pide. */
    public static PasswordAuthentication requestPasswordAuthentication(
            String host, InetAddress addr, int port, String protocol, String prompt,
            String scheme, URL url, RequestorType reqType) {
        return requestPasswordAuthentication(
                theAuthenticator, host, addr, port, protocol, prompt, scheme, url, reqType);
    }

    /**
     * Como la anterior, pero preguntandole a un authenticator concreto en vez de al instalado.
     *
     * <p>Existe para que una biblioteca pueda tener su propio authenticator sin pisarle el global a
     * la aplicacion que la usa.
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

    /** Le pide credenciales a **esta** instancia, sin pasar por el authenticator instalado. */
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

    /** El nombre del host que pide, o null si solo se dio la direccion. */
    protected final String getRequestingHost() {
        return this.requestingHost;
    }

    /** La direccion del que pide, o null si solo se dio el nombre. */
    protected final InetAddress getRequestingSite() {
        return this.requestingSite;
    }

    protected final int getRequestingPort() {
        return this.requestingPort;
    }

    /** El protocolo de la conexion ("http", "ftp"...). */
    protected final String getRequestingProtocol() {
        return this.requestingProtocol;
    }

    /** El texto que el servidor mando para mostrarle al usuario (el "realm" de HTTP Basic). */
    protected final String getRequestingPrompt() {
        return this.requestingPrompt;
    }

    /** El esquema de autenticacion ("basic", "digest"...). */
    protected final String getRequestingScheme() {
        return this.requestingScheme;
    }

    /**
     * Lo que la subclase pisa para entregar credenciales. Devolver null significa "no las doy", y
     * es la respuesta por defecto: un authenticator que no pisa nada no autentica nada.
     */
    protected PasswordAuthentication getPasswordAuthentication() {
        return null;
    }

    /** La URL que gatillo el pedido, o null si no se dio. */
    protected URL getRequestingURL() {
        return this.requestingURL;
    }

    /** Si el que pide es el servidor o un proxy. */
    protected RequestorType getRequestorType() {
        return this.requestingAuthType;
    }
}
