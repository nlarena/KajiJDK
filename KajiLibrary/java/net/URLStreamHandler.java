package java.net;

import java.io.IOException;

// El que sabe hablar UN protocolo: como se escribe una URL suya, y como se abre.
//
// Es la pieza que hace extensible a `java.net.URL`. `URL` no sabe nada de `http` ni de `file`:
// delega en el manejador del esquema, y por eso agregar un protocolo nuevo es escribir uno de estos
// y registrarlo con una `URLStreamHandlerFactory`.
//
// ===========================================================================================
// QUE ENTRA Y QUE NO
// ===========================================================================================
//
// Entra la mitad que **compara y escribe** URLs, que es computo puro sobre las partes ya
// separadas: `equals`, `hashCode`, `sameFile`, `hostsEqual`, `toExternalForm`, `getDefaultPort`.
// Estan completas y con el mismo algoritmo del JDK.
//
// `openConnection(URL)` queda **abstracto**, como en el JDK: es el metodo del que cuelga todo el
// transporte, y declararlo abstracto no promete nada -- lo escribe quien implemente un protocolo.
// `openConnection(URL, Proxy)` tira `UnsupportedOperationException`, que es **literalmente lo que
// hace la clase base del JDK** ("Method not implemented."): un manejador que no sepa de proxies no
// tiene que saber.
//
// NO ENTRAN TRES, y la razon es la misma para los tres: `parseURL(URL,String,int,int)` y las dos
// sobrecargas de `setURL`. Existen solo para **mutar los campos internos de una `URL`** -- son el
// unico camino por el que el JDK deja que un tercero escriba adentro de una URL ya construida.
//
// La `java.net.URL` de este arbol es **inmutable**: guarda la `URI` que la parseo y nada mas, y
// toda su descomposicion sale de ahi (la cabecera de `URL.java` explica por que se delego el
// parsing en `URI` en vez de tener dos parsers de la misma gramatica). No hay campos que escribir,
// y agregarlos para que estos tres metodos existan seria romper la inmutabilidad de una clase que
// se usa en todos lados para mejorar un conteo en tres.
//
// La consecuencia hay que decirla derecho: **un `URLStreamHandler` escrito aca no puede parsear un
// esquema con una sintaxis propia**. Puede compararlas, escribirlas y abrirlas; el parsing lo hace
// `URI` para todos por igual. Faltando los tres metodos, un manejador que los necesite **no
// compila**, que es cuando conviene enterarse.
//
// `getHostAddress` si esta, y devuelve null siempre: su contrato es "la direccion del host, o null
// si no se conoce", y aca no se conoce porque no hay resolutor (ver la cabecera de `InetAddress`).
// Null es la respuesta verdadera, no un tapon -- y el JDK devuelve null exactamente igual cuando el
// nombre no resuelve.
public abstract class URLStreamHandler {

    public URLStreamHandler() {
    }

    /**
     * Abre la conexion a {@code u}.
     *
     * <p>Abstracto: es lo unico que esta clase no puede saber por su cuenta.
     */
    protected abstract URLConnection openConnection(URL u) throws IOException;

    /**
     * Abre la conexion a {@code u} saliendo por {@code p}.
     *
     * <p>La clase base **no lo soporta y lo dice**, con este mismo texto, en el JDK: un manejador
     * que sepa usar proxies lo sobreescribe.
     *
     * @throws UnsupportedOperationException siempre, en la clase base
     */
    protected URLConnection openConnection(URL u, Proxy p) throws IOException {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    /**
     * El puerto que se usa cuando la URL no dice ninguno, o -1 si el protocolo no tiene uno.
     *
     * <p>-1 en la clase base: un protocolo generico no tiene puerto por convencion.
     */
    protected int getDefaultPort() {
        return -1;
    }

    /**
     * Si las dos URLs nombran el mismo recurso, fragmento incluido.
     *
     * <p>Es {@link #sameFile} mas la comparacion del fragmento. Que el fragmento cuente aca y no en
     * `sameFile` es la diferencia entre las dos: `#seccion2` es otra parte del mismo archivo.
     */
    protected boolean equals(URL u1, URL u2) {
        String ref1 = u1.getRef();
        String ref2 = u2.getRef();
        boolean refsIguales = ref1 == null ? ref2 == null : ref1.equals(ref2);
        return refsIguales && this.sameFile(u1, u2);
    }

    /**
     * El hash que corresponde a {@link #equals(URL, URL)}: suma de las partes.
     *
     * <p>Suma y no combinacion posicional, igual que el JDK. El host entra en minusculas, porque un
     * nombre de host no distingue mayusculas y dos escrituras del mismo host tienen que dar el
     * mismo numero.
     */
    protected int hashCode(URL u) {
        int h = 0;
        String protocol = u.getProtocol();
        if (protocol != null) {
            h = h + protocol.hashCode();
        }
        InetAddress addr = this.getHostAddress(u);
        if (addr != null) {
            h = h + addr.hashCode();
        } else {
            String host = u.getHost();
            if (host != null) {
                h = h + host.toLowerCase().hashCode();
            }
        }
        String file = u.getFile();
        if (file != null) {
            h = h + file.hashCode();
        }
        h = h + (u.getPort() == -1 ? this.getDefaultPort() : u.getPort());
        String ref = u.getRef();
        if (ref != null) {
            h = h + ref.hashCode();
        }
        return h;
    }

    /**
     * Si las dos URLs nombran el mismo archivo, **sin** mirar el fragmento.
     *
     * <p>Compara protocolo, host, puerto efectivo y ruta. El puerto "efectivo" es el que trae la
     * URL o, si no trae, el del protocolo: por eso {@code http://x/} y {@code http://x:80/} son el
     * mismo archivo.
     */
    protected boolean sameFile(URL u1, URL u2) {
        String p1 = u1.getProtocol();
        String p2 = u2.getProtocol();
        if (!(p1 == null ? p2 == null : p1.equalsIgnoreCase(p2))) {
            return false;
        }
        String f1 = u1.getFile();
        String f2 = u2.getFile();
        if (!(f1 == null ? f2 == null : f1.equals(f2))) {
            return false;
        }
        int port1 = u1.getPort() == -1 ? this.getDefaultPort() : u1.getPort();
        int port2 = u2.getPort() == -1 ? this.getDefaultPort() : u2.getPort();
        if (port1 != port2) {
            return false;
        }
        return this.hostsEqual(u1, u2);
    }

    /**
     * Si las dos URLs apuntan al mismo host.
     *
     * <p>El JDK compara primero las direcciones IP --para que un nombre y su IP resulten el mismo
     * host-- y recien si alguna no resuelve compara los nombres. Aca {@link #getHostAddress}
     * siempre da null, asi que la comparacion es siempre por nombre, sin distinguir mayusculas.
     *
     * <p>Eso hace esta comparacion **mas estricta** que la del JDK, nunca mas laxa: puede decir que
     * dos URLs son de hosts distintos donde el JDK diria que son el mismo, y no al reves.
     */
    protected boolean hostsEqual(URL u1, URL u2) {
        InetAddress a1 = this.getHostAddress(u1);
        InetAddress a2 = this.getHostAddress(u2);
        if (a1 != null && a2 != null) {
            return a1.equals(a2);
        }
        String h1 = u1.getHost();
        String h2 = u2.getHost();
        if (h1 != null && h2 != null) {
            return h1.equalsIgnoreCase(h2);
        }
        return h1 == null && h2 == null;
    }

    /**
     * La direccion IP del host de {@code u}, o null si no se conoce.
     *
     * <p>Siempre null en KajiJDK: no hay resolutor de nombres. Null es una respuesta que el
     * contrato ya contempla --el JDK la da cuando el nombre no resuelve-- y los dos llamadores de
     * este metodo, {@link #hashCode} y {@link #hostsEqual}, tienen su camino alternativo escrito.
     */
    protected InetAddress getHostAddress(URL u) {
        return null;
    }

    /**
     * La URL escrita como texto.
     *
     * <p>Rearma {@code protocolo://autoridad + archivo + #fragmento}. Los pedazos que faltan se
     * omiten enteros, con su separador: una URL sin autoridad no lleva las dos barras.
     */
    protected String toExternalForm(URL u) {
        StringBuilder b = new StringBuilder();
        b.append(u.getProtocol()).append(':');
        String authority = u.getAuthority();
        if (authority != null && authority.length() > 0) {
            b.append("//").append(authority);
        }
        String file = u.getFile();
        if (file != null) {
            b.append(file);
        }
        String ref = u.getRef();
        if (ref != null) {
            b.append('#').append(ref);
        }
        return b.toString();
    }

    /**
     * Parsea {@code spec} y le carga los componentes a {@code u}.
     *
     * <p>Es el protocolo con el que el JDK deja que un manejador entienda un esquema propio: la
     * {@link URL} llega vacia, el manejador la parsea a su manera y la llena con {@link #setURL}.
     *
     * <h2>Por que aca no puede andar</h2>
     *
     * <p>Porque la {@link URL} de esta biblioteca es <strong>inmutable</strong>: guarda un
     * {@link java.net.URI} y una cadena, los dos {@code final}. No hay nada que llenar despues de
     * construida, asi que el protocolo de "parsear y cargar" no tiene donde apoyarse.
     *
     * <p>No es una omision que se arregle escribiendo mas: seria cambiar la representacion de
     * {@code URL}. Queda declarado con la firma exacta —una subclase que lo sobrescriba compila— y
     * lo que declina es la version heredada, que mentiria si no hiciera nada.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    protected void parseURL(URL u, String spec, int start, int limit) {
        throw new UnsupportedOperationException(
                "la URL de esta biblioteca es inmutable: no se la puede llenar despues de creada");
    }

    /**
     * Le carga los componentes a {@code u}.
     *
     * @throws UnsupportedOperationException siempre — ver {@link #parseURL}
     */
    protected void setURL(URL u, String protocol, String host, int port, String authority,
            String userInfo, String path, String query, String ref) {
        throw new UnsupportedOperationException(
                "la URL de esta biblioteca es inmutable: no se la puede llenar despues de creada");
    }

    /**
     * La forma vieja, de antes de que una URL distinguiera autoridad de host.
     *
     * @deprecated usar la de nueve argumentos, que separa {@code authority}, {@code userInfo} y
     *     {@code query} en vez de meterlos en {@code file}
     * @throws UnsupportedOperationException siempre — ver {@link #parseURL}
     */
    @Deprecated(since = "1.2")
    protected void setURL(URL u, String protocol, String host, int port, String file, String ref) {
        throw new UnsupportedOperationException(
                "la URL de esta biblioteca es inmutable: no se la puede llenar despues de creada");
    }
}
