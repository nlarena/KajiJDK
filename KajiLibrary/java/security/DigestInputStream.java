package java.security;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

// Un stream que le va pasando al digest todo lo que se lee de el.
//
// Sirve para hashear algo sin leerlo dos veces ni tenerlo entero en memoria: se lee normal, y al
// terminar el digest ya esta calculado. La trampa es que **solo cuenta lo que se leyo**: si el
// consumidor corta antes de fin de archivo, o hace `skip`, el digest es el del prefijo que paso
// por aca, no el del archivo. Esta clase no puede detectar la diferencia, y comparar ese digest
// contra el del archivo completo da distinto sin que nada haya fallado.
//
// `skip()` no se sobreescribe —lo hereda de `FilterInputStream`, que lee y descarta— asi que los
// bytes salteados **si** entran al digest. Es el comportamiento del JDK y es el menos sorpresivo:
// saltear no deberia cambiar el hash del contenido recorrido.
//
// Los dos `read` declaran `throws IOException`, igual que en el JDK. Hasta hace poco no podian:
// `java.io.FilterInputStream` de esta biblioteca no lo declaraba en los suyos y Java prohibe que
// una subclase declare **mas** excepciones chequeadas que el metodo que sobreescribe. Ahora que
// java.io alineo sus firmas, la diferencia desaparecio.
public class DigestInputStream extends FilterInputStream {

    // Protegido, como en el JDK: una subclase puede necesitar tocarlo.
    protected MessageDigest digest;

    // Si el digest esta escuchando. Empieza prendido.
    private boolean on = true;

    public DigestInputStream(InputStream stream, MessageDigest digest) {
        super(stream);
        this.setMessageDigest(digest);
    }

    public MessageDigest getMessageDigest() {
        return this.digest;
    }

    // Cambiar el digest a mitad de camino es legal y a veces es el punto: se lee una cabecera con
    // uno y el cuerpo con otro.
    public void setMessageDigest(MessageDigest digest) {
        this.digest = digest;
    }

    @Override
    public int read() throws IOException {
        int ch = this.in.read();
        if (this.on && ch != -1) {
            this.digest.update((byte) ch);
        }
        return ch;
    }

    // Alimenta solo los bytes que **realmente** se leyeron, no `len`.
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = this.in.read(b, off, len);
        if (this.on && result > 0) {
            this.digest.update(b, off, result);
        }
        return result;
    }

    // Prende o apaga la alimentacion del digest.
    //
    // Es lo que permite hashear un pedazo del stream y no otro: por ejemplo, saltearse un campo de
    // firma que esta embebido en el mismo archivo que se esta verificando.
    public void on(boolean on) {
        this.on = on;
    }

    @Override
    public String toString() {
        return "[Digest Input Stream] " + this.digest.toString();
    }
}
