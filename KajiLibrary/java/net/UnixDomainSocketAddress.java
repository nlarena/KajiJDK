package java.net;

import java.nio.file.Path;

// La direccion de un socket de dominio Unix: una ruta del sistema de archivos.
//
// Es la contraparte de `InetSocketAddress` para procesos que hablan **dentro de la misma maquina**.
// No hay direccion IP ni puerto porque no hay red de por medio: el nombre es un archivo, y los
// permisos del archivo son los permisos de la conexion. Eso ultimo es la razon practica de que
// existan: dar acceso a un servicio local es un `chmod`, no una regla de firewall.
//
// La ruta vacia es legal y significa una direccion **sin nombre**, la que tiene un socket que
// todavia no se ato a ninguna ruta.
//
// La clase es un envoltorio inmutable de un `Path`. Nada omitido: describir la direccion no es
// abrir el socket, y solo lo primero esta aca.
public final class UnixDomainSocketAddress extends SocketAddress {

    private static final long serialVersionUID = 92902496589351698L;

    private final transient Path path;

    private UnixDomainSocketAddress(Path path) {
        this.path = path;
    }

    /** La direccion para esa ruta, escrita como texto. */
    public static UnixDomainSocketAddress of(String pathname) {
        return of(Path.of(pathname));
    }

    /** La direccion para esa ruta. */
    public static UnixDomainSocketAddress of(Path path) {
        return new UnixDomainSocketAddress(path);
    }

    public Path getPath() {
        return this.path;
    }

    public int hashCode() {
        return this.path.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof UnixDomainSocketAddress)) {
            return false;
        }
        return this.path.equals(((UnixDomainSocketAddress) o).path);
    }

    public String toString() {
        return this.path.toString();
    }
}
