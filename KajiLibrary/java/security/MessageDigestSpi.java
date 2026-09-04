package java.security;

import java.nio.ByteBuffer;

// La cara que le da un proveedor a un algoritmo de digest.
//
// La separacion entre este tipo y `MessageDigest` es el patron Service Provider Interface, y su
// razon de ser es que las dos caras cambian por motivos distintos: `MessageDigest` es lo que
// promete el API y no se puede tocar, mientras que esto es lo que escribe quien implementa un
// algoritmo nuevo. Un proveedor que agrega un hash escribe una subclase de esto y no ve nunca la
// otra mitad.
//
// (Detalle historico que confunde a todos: `MessageDigest extends MessageDigestSpi`. No es un
// error de diseño de la biblioteca sino un atajo deliberado — permite que una implementacion que
// vive dentro del JDK sea las dos cosas a la vez y se ahorre el objeto delegado. Cuando el spi y
// la cara publica son objetos distintos, `MessageDigest` usa un delegado interno.)
public abstract class MessageDigestSpi {

    public MessageDigestSpi() {
    }

    // El largo del digest en bytes, o 0 si esta implementacion no lo sabe de antemano.
    //
    // Devuelve 0 y no lanza porque este metodo se agrego despues que la clase: una subclase vieja
    // no lo escribe, y 0 es como se dice "no se". Toda implementacion nueva deberia sobreescribirlo.
    protected int engineGetDigestLength() {
        return 0;
    }

    protected abstract void engineUpdate(byte input);

    protected abstract void engineUpdate(byte[] input, int offset, int len);

    // Consume lo que queda en el buffer y lo deja posicionado al final.
    //
    // Concreto y no abstracto por compatibilidad: la version por arreglo alcanza para cumplirlo, y
    // una implementacion que sepa aprovechar un buffer directo lo sobreescribe.
    protected void engineUpdate(ByteBuffer input) {
        if (!input.hasRemaining()) {
            return;
        }
        if (input.hasArray()) {
            byte[] b = input.array();
            int ofs = input.arrayOffset();
            int pos = input.position();
            int lim = input.limit();
            this.engineUpdate(b, ofs + pos, lim - pos);
            input.position(lim);
            return;
        }
        int n = input.remaining();
        byte[] tmp = new byte[n < 4096 ? n : 4096];
        while (n > 0) {
            int chunk = n < tmp.length ? n : tmp.length;
            input.get(tmp, 0, chunk);
            this.engineUpdate(tmp, 0, chunk);
            n = n - chunk;
        }
    }

    protected abstract byte[] engineDigest();

    // Escribe el digest en `buf` y devuelve cuantos bytes escribio.
    //
    // La implementacion base calcula el digest completo y lo copia: no hay forma generica de
    // producir medio digest, y por eso `len` menor al largo real es un error y no una truncacion
    // silenciosa. Un digest truncado sin que el llamador lo sepa es exactamente el tipo de dato
    // que despues se compara contra otro y coincide de mas.
    protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
        byte[] digest = this.engineDigest();
        if (len < digest.length) {
            throw new DigestException("partial digests not returned");
        }
        if (buf.length - offset < digest.length) {
            throw new DigestException("insufficient space in the output buffer to store the digest");
        }
        System.arraycopy(digest, 0, buf, offset, digest.length);
        return digest.length;
    }

    protected abstract void engineReset();

    // Solo funciona si la subclase declara `Cloneable`. Clonar un digest a medio camino es la
    // unica forma de sacar el hash de un prefijo sin volver a leer los datos.
    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}
