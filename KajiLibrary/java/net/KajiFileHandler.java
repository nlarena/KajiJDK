package java.net;

import java.io.IOException;

/**
 * KajiLibrary's java.net.KajiFileHandler -- el handler de `file:`, y el unico que viene puesto.
 *
 * <p>Viene puesto porque es el unico protocolo que esta VM puede recorrer de punta a punta: leer un
 * archivo es `java.io`, que anda. `http:` y compania necesitan un socket, y esta VM no tiene ninguno
 * --lo verificaron dos sesiones buscando en el codigo de la VM--, asi que para esos no hay handler y
 * `URL.openConnection()` lo dice en vez de fingir.
 *
 * <p>Que exista **uno** cambia la naturaleza de `openConnection`: deja de ser un metodo que solo
 * podria fallar y pasa a ser uno que funciona para lo que puede y falla, con nombre y apellido, para
 * lo que no.
 */
final class KajiFileHandler extends URLStreamHandler {

    protected URLConnection openConnection(URL u) throws IOException {
        return new KajiFileConnection(u);
    }

    protected int getDefaultPort() {
        // `file:` no tiene puerto. -1 es lo que el contrato pide para eso.
        return -1;
    }
}

/**
 * KajiLibrary's java.net.KajiFileConnection -- una conexion a un archivo local.
 *
 * <p>`connect()` no abre nada: comprueba que el archivo **este**, que es todo lo que "conectar"
 * significa para un archivo local. Abrir el flujo recien en `getInputStream` es lo que hace que
 * conectarse y leer sean dos pasos, como en cualquier otro protocolo.
 */
final class KajiFileConnection extends URLConnection {

    private java.io.File archivo;

    KajiFileConnection(URL u) {
        super(u);
    }

    public void connect() throws IOException {
        if (this.connected) {
            return;
        }
        String camino = this.getURL().getPath();
        if (camino == null || camino.length() == 0) {
            throw new IOException("la URL no nombra ningun archivo: " + this.getURL());
        }
        // Un `file:` bien formado lleva el camino con barras y arrancando en `/`. En Windows el
        // camino real es `C:/...`, asi que la barra de adelante sobra: `/C:/x` no existe y `C:/x` si.
        if (camino.length() > 2 && camino.charAt(0) == '/' && camino.charAt(2) == ':') {
            camino = camino.substring(1);
        }
        this.archivo = new java.io.File(camino);
        if (!this.archivo.exists()) {
            throw new java.io.FileNotFoundException(camino);
        }
        this.connected = true;
    }

    public java.io.InputStream getInputStream() throws IOException {
        this.connect();
        return new java.io.FileInputStream(this.archivo);
    }

    /** El tamano del archivo, o -1 si no entra en un `int`. Es lo que `getContentLength` promete. */
    public int getContentLength() {
        long n = this.getContentLengthLong();
        return n > (long) Integer.MAX_VALUE ? -1 : (int) n;
    }

    public long getContentLengthLong() {
        try {
            this.connect();
        } catch (IOException e) {
            return -1L;
        }
        return this.archivo.length();
    }

    public long getLastModified() {
        try {
            this.connect();
        } catch (IOException e) {
            return 0L;
        }
        return this.archivo.lastModified();
    }
}
