import java.nio.channels.spi.AsynchronousChannelProvider;
import java.nio.channels.spi.SelectorProvider;
import java.util.ServiceConfigurationError;

// El camino de fallo de los dos `provider()` de java.nio.channels.spi.
//
// Va en su propio archivo y no dentro de SpiProvTest porque las dos implementaciones --la de aca y
// la de la JVM real-- resuelven el proveedor **una sola vez por VM**: despues de la primera
// busqueda no hay forma de volver a mirar. Un solo chequeo negativo por proveedor y por corrida.
//
// Se prueba solo el caso "la clase no esta": es el unico en el que las dos VMs coinciden en el
// tipo de error (ServiceConfigurationError, sin envolver). El caso "la clase esta pero no es un
// proveedor" divergo a proposito y esta documentado en el encabezado de SelectorProvider.
//
// run() devuelve -1 si todo anduvo; si no, el numero del chequeo que fallo.
public class SpiProvErrTest {

    public static int run() {
        // ---- 1: propiedad que nombra una clase inexistente -> ServiceConfigurationError -------
        System.setProperty("java.nio.channels.spi.SelectorProvider", "spi.no.Existe");
        try {
            SelectorProvider.provider();
            return 1;
        } catch (ServiceConfigurationError esperada) {
            // Bien.
        }

        // ---- 2: lo mismo para el proveedor asincronico ---------------------------------------
        System.setProperty("java.nio.channels.spi.AsynchronousChannelProvider", "spi.no.Existe");
        try {
            AsynchronousChannelProvider.provider();
            return 2;
        } catch (ServiceConfigurationError esperada) {
            // Bien.
        }

        return -1;
    }
}
