import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.nio.channels.spi.SelectorProvider;

// Los dos `provider()` estaticos de java.nio.channels.spi y las tres sobrecargas con
// ProtocolFamily. Corre igual en KajiJDK y en la JVM real: no afirma nada sobre el proveedor por
// omision de la plataforma --que en una hay y en la otra no-- sino solo sobre el escalon de la
// propiedad de sistema, que es identico en las dos.
//
// El orden importa: la propiedad se pone ANTES de la primera llamada a provider(), porque las dos
// implementaciones cachean el resultado de la primera busqueda.
//
// run() devuelve -1 si todo anduvo; si no, el numero del chequeo que fallo.
public class SpiProvTest {

    public static int run() {
        // ---- 1: openSocketChannel(ProtocolFamily) hereda la omision que tira ------------------
        // Se prueba sobre una instancia directa, sin pasar por provider(), para que este chequeo
        // no dependa del cacheo ni del orden.
        SpiProvSel suelto = new SpiProvSel();
        try {
            suelto.openSocketChannel(StandardProtocolFamily.INET);
            return 1;
        } catch (UnsupportedOperationException esperada) {
            if (!"Protocol family not supported".equals(esperada.getMessage())) {
                return 2;
            }
        } catch (IOException e) {
            return 3;
        }

        // ---- 2: openServerSocketChannel(ProtocolFamily), lo mismo ----------------------------
        try {
            suelto.openServerSocketChannel(StandardProtocolFamily.INET6);
            return 4;
        } catch (UnsupportedOperationException esperada) {
            if (!"Protocol family not supported".equals(esperada.getMessage())) {
                return 5;
            }
        } catch (IOException e) {
            return 6;
        }

        // ---- 3: openDatagramChannel(ProtocolFamily) es una sobrecarga aparte -----------------
        // Que la abstracta con familia exista de verdad y no colapse con la de cero argumentos:
        // las dos redefiniciones dejan una marca distinta.
        try {
            suelto.openDatagramChannel();
            if (suelto.ultima != 1) {
                return 7;
            }
            suelto.openDatagramChannel(StandardProtocolFamily.UNIX);
            if (suelto.ultima != 2) {
                return 8;
            }
            if (!"UNIX".equals(suelto.familia)) {
                return 9;
            }
        } catch (IOException e) {
            return 10;
        }

        // ---- 4: SelectorProvider.provider() toma la propiedad de sistema ---------------------
        System.setProperty("java.nio.channels.spi.SelectorProvider", "SpiProvSel");
        SelectorProvider sp1 = SelectorProvider.provider();
        if (!(sp1 instanceof SpiProvSel)) {
            return 11;
        }

        // ---- 5: y cachea: la segunda llamada devuelve el mismo objeto ------------------------
        SelectorProvider sp2 = SelectorProvider.provider();
        if (sp1 != sp2) {
            return 12;
        }
        // Cambiar la propiedad despues no cambia nada: la busqueda ya se hizo.
        System.setProperty("java.nio.channels.spi.SelectorProvider", "SpiProvOtro");
        if (SelectorProvider.provider() != sp1) {
            return 13;
        }

        // ---- 6: AsynchronousChannelProvider.provider(), el mismo mecanismo -------------------
        System.setProperty("java.nio.channels.spi.AsynchronousChannelProvider", "SpiProvAsy");
        AsynchronousChannelProvider ap1 = AsynchronousChannelProvider.provider();
        if (!(ap1 instanceof SpiProvAsy)) {
            return 14;
        }
        if (AsynchronousChannelProvider.provider() != ap1) {
            return 15;
        }

        // ---- 7: el proveedor de la propiedad es una instancia nueva, no la de arriba ---------
        if (ap1 == sp1) {
            return 16;
        }
        if (sp1 == suelto) {
            return 17;
        }

        // ---- 8: inheritedChannel() sigue siendo null, y ahora por el camino del proveedor ----
        try {
            if (suelto.inheritedChannel() != null) {
                return 18;
            }
        } catch (IOException e) {
            return 19;
        }

        return -1;
    }
}
