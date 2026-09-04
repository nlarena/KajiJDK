package java.net;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

// Una direccion IP.
//
// ===========================================================================================
// QUE HAY ACA Y QUE NO, Y POR QUE
// ===========================================================================================
//
// Una direccion IP son dos cosas que la gente confunde todo el tiempo: **un numero con formato** y
// **un nombre que hay que resolver**. La primera mitad es aritmetica de bytes y gramaticas de
// literales -- RFC 791 y RFC 4291 -- y se puede escribir entera aca sin tocar la red. La segunda
// necesita un resolver, o sea DNS, o sea sockets, y KajiJDK no tiene ninguno.
//
// Entonces: **el parseo y el formateo estan completos y son fieles**; la resolucion no existe y no
// se simula. `getByName` de un literal devuelve la direccion; de un nombre que no sea "localhost"
// tira `UnknownHostException`, que es exactamente lo que el JDK hace cuando el DNS no contesta. Eso
// no es una mentira: es el resultado honesto de no tener resolver, y el tipo de la excepcion ya
// estaba en el contrato.
//
// Lo que **no** se declara:
//
// ===========================================================================================
// `java.net.IDN` NO ESTA, y este es su lugar
// ===========================================================================================
//
// `IDN` convierte un nombre de host internacionalizado en uno que se pueda resolver --que es lo que
// hace esta clase con el resultado-- asi que su ausencia se dice aca.
//
// `toASCII` **no es** Punycode. Es *nameprep* (RFC 3491) y **despues** Punycode, y el primer paso es
// el que no se puede escribir: plegado de mayusculas completo, normalizacion NFKC, y las tablas de
// caracteres prohibidos. No es un detalle academico, se ve en un ejemplo de dos palabras: el JDK
// convierte `strasse.de` y `stra{eszett}.de` **a la misma cadena**, porque el plegado completo manda
// la eszett a `ss`. Un `toLowerCase` no hace eso, y la version que lo usara daria dos nombres
// distintos para el mismo dominio -- que en un resolutor es exactamente el error que no se perdona.
//
// Es el mismo muro que deja a `java.text.Normalizer` sin NFKC y a `java.text` sin sus cuatro
// ultimos miembros: hacen falta las tablas de Unicode, y este arbol no las tiene. Escribir `IDN`
// sin ellas seria un metodo que contesta bien los nombres ASCII --donde no hay nada que hacer-- y
// mal justamente los que motivan que la clase exista.
//
// Punycode solo, que si se puede escribir exacto, no alcanza: `toASCII` no lo promete a secas.

// `isReachable(int)` **prueba de verdad**: un TCP al puerto 7 donde un rechazo
// cuenta como respuesta, porque el RST lo manda el host. Es el mismo camino de reserva del JDK
// cuando no puede mandar un ICMP, que es lo normal --un ping crudo necesita permisos que un proceso
// comun no tiene--. Antes contestaba `false` siempre, que era legal pero inutil; dejo de serlo
// cuando la VM aprendio TCP.
//
// **Lo unico observable que lo separa del JDK es el tiempo**, y conviene saberlo antes de elegir un
// plazo: en Windows el sistema tarda unos dos segundos en reportar un rechazo de TCP, asi que un
// host vivo con el puerto cerrado necesita un plazo de al menos eso para dar `true`. El JDK, cuando
// puede, manda un ICMP y contesta en el acto. La respuesta es la misma; lo que cambia es cuanto
// hay que esperarla.
//
// La clase es concreta y con constructor de paquete, como en el JDK: no se instancia nunca
// directamente, toda instancia es un `Inet4Address` o un `Inet6Address`. Los metodos de aca son los
// valores neutros que las subclases pisan -- misma estructura que el JDK, y por la misma razon: el
// tipo comun tiene que poder nombrarse en las firmas sin comprometerse con una familia.
public class InetAddress implements Serializable {

    private static final long serialVersionUID = 3286316764910316507L;

    // Los bytes de la direccion: cuatro para IPv4, dieciseis para IPv6. En la clase base es null,
    // porque la base no representa ninguna direccion concreta.
    final byte[] addr;

    // El nombre con el que se creo, o null si es anonima. **null no es lo mismo que ""**: una
    // direccion nacida de un literal no tiene nombre, y `toString` la imprime como "/1.2.3.4".
    // Confundir los dos era el bug de la version anterior de este archivo.
    final String hostName;

    InetAddress(String hostName, byte[] addr) {
        this.hostName = hostName;
        this.addr = addr;
    }

    /** Si es una direccion multicast. */
    public boolean isMulticastAddress() {
        return false;
    }

    /** Si es la direccion comodin ("cualquiera de las locales"). */
    public boolean isAnyLocalAddress() {
        return false;
    }

    /** Si es una direccion de loopback. */
    public boolean isLoopbackAddress() {
        return false;
    }

    /** Si es link-local (valida solo dentro del enlace fisico). */
    public boolean isLinkLocalAddress() {
        return false;
    }

    /** Si es site-local (el rango "privado"). */
    public boolean isSiteLocalAddress() {
        return false;
    }

    /** Si es multicast de alcance global. */
    public boolean isMCGlobal() {
        return false;
    }

    /** Si es multicast de alcance nodo. */
    public boolean isMCNodeLocal() {
        return false;
    }

    /** Si es multicast de alcance enlace. */
    public boolean isMCLinkLocal() {
        return false;
    }

    /** Si es multicast de alcance sitio. */
    public boolean isMCSiteLocal() {
        return false;
    }

    /** Si es multicast de alcance organizacion. */
    public boolean isMCOrgLocal() {
        return false;
    }

    /**
     * Si el host esta accesible. Siempre {@code false}: no hay red desde donde probar.
     *
     * <p>Se declara igual porque "no accesible" es una respuesta legal de este metodo -- no promete
     * llegar, promete informar si llego.
     *
     * @throws IllegalArgumentException si {@code timeout} es negativo
     */
    public boolean isReachable(int timeout) throws IOException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        if (this.addr == null) {
            // La clase base no representa ninguna direccion concreta: no hay a quien preguntarle.
            return false;
        }
        // Cero significa "sin limite" en el contrato. Ninguna espera de verdad puede ser infinita
        // aca, asi que se traduce a un plazo largo y explicito en vez de colgar el hilo.
        return this.probar("", 0, timeout);
    }

    /**
     * Si este host contesta dentro de {@code timeout} milisegundos, probando **por esa placa** y con
     * ese limite de saltos.
     *
     * <p>Con {@code netif} null sale por donde el sistema quiera y con {@code ttl} cero usa el
     * limite por omision, que es lo que documenta el JDK; en ese caso es identico a
     * {@link #isReachable(int)}.
     *
     * <p>**Con placa o con TTL la prueba afirma menos**, y hay que decirlo: la version de un
     * parametro toma el rechazo como respuesta --un RST prueba que el host esta vivo-- y esta, que
     * tiene que armar el socket a mano para poder elegir la placa, no distingue el rechazo del
     * silencio. Un `true` sigue significando "contesto"; un `false` con placa puede ser un host vivo
     * que rechazo la conexion. Es la unica diferencia entre las dos, y no se puede evitar sin
     * reimplementar el `connect` no bloqueante de cada sistema.
     *
     * @param netif la placa por la que sale la prueba, o null
     * @param ttl el limite de saltos, o cero
     * @param timeout milisegundos; cero es "sin limite"
     * @throws IllegalArgumentException si el plazo o el ttl son negativos
     */
    public boolean isReachable(NetworkInterface netif, int ttl, int timeout) throws IOException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        if (ttl < 0) {
            throw new IllegalArgumentException("ttl can't be negative");
        }
        if (this.addr == null) {
            return false;
        }
        return this.probar(InetAddress.salidaDe(netif, this), ttl, timeout);
    }

    // Por que direccion local sale una prueba que tiene que ir por esa placa: la primera de la placa
    // que sea de la misma familia que el destino --atar una punta IPv4 a una conexion IPv6 no es una
    // peticion que se pueda cumplir--. La cadena vacia significa "que elija el sistema".
    private static String salidaDe(NetworkInterface netif, InetAddress destino) {
        if (netif == null) {
            return "";
        }
        boolean seis = destino instanceof Inet6Address;
        java.util.Enumeration<InetAddress> dirs = netif.getInetAddresses();
        while (dirs.hasMoreElements()) {
            InetAddress d = dirs.nextElement();
            if (seis == (d instanceof Inet6Address)) {
                return d.getHostAddress();
            }
        }
        // Una placa sin ninguna direccion de la familia del destino no puede llevar la prueba. Que
        // el sistema elija es mas util que fallar, y es lo que hace el JDK.
        return "";
    }

    // El cuerpo comun de las dos sobrecargas. La espera es de este lado porque el nativo no espera:
    // ver la cabecera de `jdk.internal.net.Net`.
    private boolean probar(String local, int ttl, int timeout) throws IOException {
        // Cero significa "sin limite" en el contrato. Ninguna espera de verdad puede ser infinita
        // aca, asi que se traduce a un plazo largo y explicito en vez de colgar el hilo.
        long plazo = timeout == 0 ? 30_000L : timeout;
        int sonda = jdk.internal.net.Net.reachableStart(this.getHostAddress(), local, ttl);
        if (sonda < 0) {
            return false;
        }
        try {
            long comienzo = System.currentTimeMillis();
            int r = jdk.internal.net.Net.answerPoll(sonda);
            while (r == -3) {
                if (System.currentTimeMillis() - comienzo >= plazo) {
                    // Sin respuesta dentro del plazo es exactamente lo que este metodo llama "no
                    // alcanzable": no se afirma que el host no exista, se afirma que no contesto.
                    return false;
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new java.io.InterruptedIOException("isReachable interrupted");
                }
                r = jdk.internal.net.Net.answerPoll(sonda);
            }
            return r == 1;
        } finally {
            jdk.internal.net.Net.answerFree(sonda);
        }
    }

    /** El nombre con el que se creo; si no tenia, su forma textual. */
    public String getHostName() {
        if (this.hostName != null) {
            return this.hostName;
        }
        return this.getHostAddress();
    }

    /** El nombre completamente calificado. Sin resolucion inversa, es {@link #getHostName()}. */
    public String getCanonicalHostName() {
        return this.getHostName();
    }

    /** Una copia de los bytes crudos. */
    public byte[] getAddress() {
        return null;
    }

    /** La direccion en forma textual. */
    public String getHostAddress() {
        return null;
    }

    public int hashCode() {
        return 0;
    }

    public boolean equals(Object obj) {
        return false;
    }

    public String toString() {
        return Objects.toString(this.hostName, "") + "/" + this.getHostAddress();
    }

    // ---- factorias ------------------------------------------------------------------------------

    /**
     * La direccion para {@code host} y los bytes {@code addr}, sin consultar a nadie.
     *
     * <p>Cuatro bytes dan un {@link Inet4Address}; dieciseis dan un {@link Inet6Address}, salvo que
     * sean la forma "IPv4-mapped" (::ffff:a.b.c.d), que colapsa a `Inet4Address` -- esa direccion
     * **es** una IPv4, escrita con la sintaxis de IPv6, y tratarla como v6 haria que dos objetos que
     * nombran el mismo host no fueran iguales.
     *
     * @throws UnknownHostException si {@code addr} no mide 4 ni 16
     */
    public static InetAddress getByAddress(String host, byte[] addr) throws UnknownHostException {
        if (host != null && host.length() > 0 && host.charAt(0) == '[') {
            if (host.charAt(host.length() - 1) == ']') {
                host = host.substring(1, host.length() - 1);
            }
        }
        if (addr == null) {
            throw new UnknownHostException("addr is of illegal length");
        }
        if (addr.length == Inet4Address.INADDRSZ) {
            return new Inet4Address(host, copy(addr));
        }
        if (addr.length == Inet6Address.INADDRSZ) {
            byte[] v4 = Inet6Address.convertFromIPv4MappedAddress(addr);
            if (v4 != null) {
                return new Inet4Address(host, v4);
            }
            return new Inet6Address(host, copy(addr));
        }
        throw new UnknownHostException("addr is of illegal length");
    }

    /** La direccion anonima para esos bytes. */
    public static InetAddress getByAddress(byte[] addr) throws UnknownHostException {
        return getByAddress(null, addr);
    }

    /**
     * La direccion de {@code host}.
     *
     * <p>Un literal IPv4 o IPv6 se parsea; "localhost" y la cadena vacia dan el loopback. Cualquier
     * otro nombre necesitaria un resolver, y no hay: tira {@link UnknownHostException}, que es lo
     * mismo que devuelve el JDK cuando el DNS no sabe.
     */
    public static InetAddress getByName(String host) throws UnknownHostException {
        return getAllByName(host)[0];
    }

    /**
     * Todas las direcciones de {@code host}. Sin resolver hay a lo sumo una, asi que el arreglo
     * tiene siempre un elemento (o no se llega a devolver nada).
     */
    public static InetAddress[] getAllByName(String host) throws UnknownHostException {
        if (host == null || host.length() == 0) {
            return new InetAddress[] {getLoopbackAddress()};
        }
        boolean bracketed = false;
        if (host.charAt(0) == '[') {
            if (host.length() > 2 && host.charAt(host.length() - 1) == ']') {
                host = host.substring(1, host.length() - 1);
                bracketed = true;
            } else {
                throw new UnknownHostException(host + ": invalid IPv6 address literal");
            }
        }
        // Solo se intenta leerlo como literal si empieza como uno podria empezar. Sin este filtro,
        // "beef.example" entraria al parser de IPv4 y saldria por el mismo lado, pero el filtro
        // tambien es lo que hace que un nombre que arranca con letra no hexadecimal ni se intente.
        if (host.length() > 0 && (digit(host.charAt(0), 16) != -1 || host.charAt(0) == ':')) {
            InetAddress parsed = null;
            if (!bracketed) {
                byte[] v4 = Inet4Address.textToNumericFormat(host);
                if (v4 != null) {
                    parsed = new Inet4Address(null, v4);
                }
            }
            if (parsed == null) {
                parsed = Inet6Address.parseLiteral(host, false);
            }
            if (parsed != null) {
                return new InetAddress[] {parsed};
            }
        }
        if (host.equalsIgnoreCase("localhost")) {
            return new InetAddress[] {getLoopbackAddress()};
        }
        throw new UnknownHostException(host);
    }

    /** El loopback: 127.0.0.1, con nombre "localhost". */
    public static InetAddress getLoopbackAddress() {
        return new Inet4Address("localhost", new byte[] {127, 0, 0, 1});
    }

    /**
     * La direccion que describe {@code s}, que tiene que ser un literal IPv4 o IPv6.
     *
     * <p>A diferencia de {@link #getByName}, esto no admite nombres: si no es un literal, no hay
     * nada que consultar y falla en el acto.
     *
     * @throws IllegalArgumentException si no es un literal valido
     */
    public static InetAddress ofLiteral(String s) {
        Objects.requireNonNull(s);
        byte[] v4 = Inet4Address.textToNumericFormat(s);
        if (v4 != null) {
            return new Inet4Address(null, v4);
        }
        InetAddress v6 = Inet6Address.parseLiteral(s, true);
        if (v6 == null) {
            throw invalidLiteral(s);
        }
        return v6;
    }

    /** El host local. KajiJDK no tiene identidad de red, asi que es el loopback. */
    public static InetAddress getLocalHost() throws UnknownHostException {
        return getLoopbackAddress();
    }

    // ---- utilidades compartidas por las subclases -----------------------------------------------

    static IllegalArgumentException invalidLiteral(String s) {
        return new IllegalArgumentException("Invalid IP address literal: " + s);
    }

    static byte[] copy(byte[] src) {
        byte[] out = new byte[src.length];
        int i = 0;
        while (i < src.length) {
            out[i] = src[i];
            i = i + 1;
        }
        return out;
    }

    // `Character.digit` acepta digitos de todo Unicode; para un literal IP eso seria un agujero
    // (los digitos arabigo-indicos no son digitos de una direccion), asi que se restringe a ASCII.
    static int digit(char c, int radix) {
        int v = -1;
        if (c >= '0' && c <= '9') {
            v = c - '0';
        } else if (c >= 'a' && c <= 'z') {
            v = c - 'a' + 10;
        } else if (c >= 'A' && c <= 'Z') {
            v = c - 'A' + 10;
        }
        if (v < 0 || v >= radix) {
            return -1;
        }
        return v;
    }
}
