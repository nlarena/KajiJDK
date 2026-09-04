package java.security;

import java.nio.ByteBuffer;

// Una funcion de hash criptografica, con la fabrica que la busca entre los proveedores.
//
// ===============================================================================================
// QUE HAY DE VERDAD ACA, Y QUE NO
// ===============================================================================================
//
// Esta es la unica fabrica de `java.security` en KajiLibrary que tiene algoritmos **de verdad**
// detras. `KajiProvider` registra seis, escritos de cero en esta biblioteca y verificados byte a
// byte contra los del JDK 25 y contra los vectores de las especificaciones (RFC 1321 para MD5,
// FIPS 180-4 para la familia SHA):
//
//     MD5, SHA-1, SHA-224, SHA-256, SHA-384, SHA-512
//
// Todo lo demas que se le pida a `getInstance` tira `NoSuchAlgorithmException`, que es la respuesta
// correcta: no hay proveedor que lo ofrezca. En particular **no** hay SHA-3 ni SHAKE — la
// permutacion Keccak es otro algoritmo entero y no se escribio.
//
// MD5 y SHA-1 estan **rotos para uso criptografico** y se registran igual. No es una contradiccion
// con la regla de no mentir: la clase no promete que el algoritmo sea seguro, promete que devuelve
// el digest que la especificacion define, y eso lo cumple exactamente. Se siguen necesitando para
// leer formatos viejos, checksums y HMAC heredados, y omitirlos no vuelve seguro a nadie: lo unico
// que hace es que el que los necesita se escriba una version peor.
//
// Lo que no esta: `getInstance` no lee `java.security` de disco ni descubre proveedores por
// `ServiceLoader`; la lista de proveedores es la que `Security` tiene en memoria.
public abstract class MessageDigest extends MessageDigestSpi {

    private final String algorithm;

    // De donde salio esta instancia. Lo setea la fabrica; una subclase construida a mano lo tiene
    // en null hasta que alguien la registre.
    private Provider provider;

    // Si desde el ultimo `digest()` o `reset()` entro algun byte. Solo lo usa `toString`.
    private boolean enCurso;

    protected MessageDigest(String algorithm) {
        this.algorithm = algorithm;
    }

    // Un digest del algoritmo pedido, del primer proveedor que lo ofrezca.
    //
    // El orden importa y es el de `Security.getProviders()`: gana el primero, y por eso insertar un
    // proveedor en la posicion 1 alcanza para reemplazar un algoritmo en todo el proceso.
    public static MessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("MessageDigest", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " MessageDigest not available");
    }

    public static MessageDigest getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, p);
    }

    public static MessageDigest getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("MessageDigest", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return armar(s, algorithm);
    }

    // Instancia el servicio y lo envuelve si hace falta.
    //
    // Si lo que devuelve el proveedor ya es un `MessageDigest`, se usa tal cual; si es solo un
    // `MessageDigestSpi`, se envuelve en un delegado. Los dos casos existen porque un algoritmo
    // que vive dentro de la biblioteca puede ahorrarse el objeto de mas.
    private static MessageDigest armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        MessageDigest md;
        if (o instanceof MessageDigest) {
            md = (MessageDigest) o;
        } else if (o instanceof MessageDigestSpi) {
            md = new DigestDelegado((MessageDigestSpi) o, algorithm);
        } else {
            throw new NoSuchAlgorithmException(
                "class configured for MessageDigest is not a MessageDigestSpi: " + s.getClassName());
        }
        md.provider = s.getProvider();
        return md;
    }

    // El proveedor del que salio, o null si se construyo a mano.
    public final Provider getProvider() {
        return this.provider;
    }

    // Package-private: `Security` y las fabricas del paquete necesitan poder marcarlo.
    final void setProvider(Provider p) {
        this.provider = p;
    }

    public void update(byte input) {
        this.engineUpdate(input);
        this.enCurso = true;
    }

    public void update(byte[] input, int offset, int len) {
        if (input == null) {
            throw new IllegalArgumentException("No input buffer given");
        }
        if (input.length - offset < len) {
            throw new IllegalArgumentException("Input buffer too short");
        }
        this.engineUpdate(input, offset, len);
        this.enCurso = true;
    }

    public void update(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("No input buffer given");
        }
        this.engineUpdate(input, 0, input.length);
        this.enCurso = true;
    }

    public final void update(ByteBuffer input) {
        if (input == null) {
            throw new NullPointerException();
        }
        this.engineUpdate(input);
        this.enCurso = true;
    }

    // Cierra el digest y lo devuelve. Despues de esto el objeto queda reseteado y listo para
    // volver a usarse — no hay que construir uno nuevo por mensaje.
    public byte[] digest() {
        byte[] result = this.engineDigest();
        this.enCurso = false;
        return result;
    }

    public int digest(byte[] buf, int offset, int len) throws DigestException {
        if (buf == null) {
            throw new IllegalArgumentException("No output buffer given");
        }
        if (buf.length - offset < len) {
            throw new IllegalArgumentException(
                "Output buffer too small for specified offset and length");
        }
        int numBytes = this.engineDigest(buf, offset, len);
        this.enCurso = false;
        return numBytes;
    }

    // Agrega `input` y cierra, todo junto. Es el atajo para el caso de un solo bloque de datos.
    public byte[] digest(byte[] input) {
        this.update(input);
        return this.digest();
    }

    @Override
    public String toString() {
        String nombreProv = this.provider == null ? "(no provider)" : this.provider.getName();
        String estado = this.enCurso ? "<in progress>" : "<initialized>";
        return this.algorithm + " Message Digest from " + nombreProv + ", " + estado + "\n";
    }

    // Compara dos digests **en tiempo constante** respecto de en que byte difieren.
    //
    // Este metodo es la razon por la que no alcanza con `Arrays.equals`. Un `equals` normal corta
    // en la primera diferencia, y esa diferencia de tiempo es medible: quien controla uno de los
    // dos arreglos puede ir descubriendo el otro byte por byte, con 256 intentos por posicion en
    // vez de 256^n por el total. Aca se recorren siempre todos los bytes y se acumula con OR.
    //
    // El largo si se filtra —no hay forma de no filtrarlo— pero el largo de un digest es publico.
    public static boolean isEqual(byte[] digesta, byte[] digestb) {
        if (digesta == digestb) {
            return true;
        }
        if (digesta == null || digestb == null) {
            return false;
        }
        int lenA = digesta.length;
        int lenB = digestb.length;
        if (lenB == 0) {
            return lenA == 0;
        }
        int result = 0;
        result = result | (lenA - lenB);

        // Se recorre siempre `lenA` entero. Cuando `i` se pasa de `lenB` el indice se colapsa a 0
        // —el shift de signo da 0 en vez de 1— asi que se relee un byte ya visto en lugar de
        // salirse del arreglo, y el bucle no cambia de largo segun los datos.
        int i = 0;
        while (i < lenA) {
            int indexB = ((i - lenB) >>> 31) * i;
            result = result | (digesta[i] ^ digestb[indexB]);
            i = i + 1;
        }
        return result == 0;
    }

    public void reset() {
        this.engineReset();
        this.enCurso = false;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    // El largo del digest en bytes.
    public final int getDigestLength() {
        return this.engineGetDigestLength();
    }

    // Un clon con el mismo estado intermedio, si la implementacion lo permite.
    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}

// La cara publica de un spi que no es a la vez un `MessageDigest`.
//
// Reenvia cada `engineX` al spi envuelto. Existe solo para el caso en que un proveedor externo
// entrega un `MessageDigestSpi` pelado; los digests de esta biblioteca extienden `MessageDigest`
// directamente y no pasan por aca.
final class DigestDelegado extends MessageDigest implements Cloneable {

    private MessageDigestSpi spi;

    DigestDelegado(MessageDigestSpi spi, String algorithm) {
        super(algorithm);
        this.spi = spi;
    }

    @Override
    protected int engineGetDigestLength() {
        return this.spi.engineGetDigestLength();
    }

    @Override
    protected void engineUpdate(byte input) {
        this.spi.engineUpdate(input);
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        this.spi.engineUpdate(input, offset, len);
    }

    @Override
    protected void engineUpdate(ByteBuffer input) {
        this.spi.engineUpdate(input);
    }

    @Override
    protected byte[] engineDigest() {
        return this.spi.engineDigest();
    }

    @Override
    protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
        return this.spi.engineDigest(buf, offset, len);
    }

    @Override
    protected void engineReset() {
        this.spi.engineReset();
    }

    // El clon tiene que llevarse **su propio** spi: dos delegados compartiendo el spi serian el
    // mismo digest con dos nombres, que es justo lo contrario de lo que se pide al clonar.
    @Override
    public Object clone() throws CloneNotSupportedException {
        DigestDelegado copia = (DigestDelegado) super.clone();
        copia.spi = (MessageDigestSpi) this.spi.clone();
        return copia;
    }
}
