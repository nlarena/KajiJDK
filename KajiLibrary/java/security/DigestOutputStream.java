package java.security;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

// El espejo de `DigestInputStream`: hashea todo lo que se escribe mientras se escribe.
//
// Ojo con un detalle que se hereda de `FilterOutputStream` y que aca importa mas que alla: la
// version de tres argumentos de `write` **si** esta sobreescrita, asi que un `write(byte[])`
// —que `FilterOutputStream` implementa escribiendo byte por byte en algunas versiones— no
// duplica ni saltea nada. Los dos caminos alimentan el digest exactamente una vez por byte.
//
// Los dos `write` declaran `throws IOException`, igual que en el JDK y por el mismo motivo que se
// explica en `DigestInputStream`: la restriccion que lo impedia era de `java.io.FilterOutputStream`
// y ya no esta.
public class DigestOutputStream extends FilterOutputStream {

    protected MessageDigest digest;

    private boolean on = true;

    public DigestOutputStream(OutputStream stream, MessageDigest digest) {
        super(stream);
        this.setMessageDigest(digest);
    }

    public MessageDigest getMessageDigest() {
        return this.digest;
    }

    public void setMessageDigest(MessageDigest digest) {
        this.digest = digest;
    }

    @Override
    public void write(int b) throws IOException {
        this.out.write(b);
        if (this.on) {
            this.digest.update((byte) b);
        }
    }

    // Se escribe primero y se hashea despues: si la escritura falla, lo que no llego al destino
    // tampoco entra al digest.
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.out.write(b, off, len);
        if (this.on) {
            this.digest.update(b, off, len);
        }
    }

    public void on(boolean on) {
        this.on = on;
    }

    @Override
    public String toString() {
        return "[Digest Output Stream] " + this.digest.toString();
    }
}
