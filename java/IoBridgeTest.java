// Los miembros que el merge de java.lang destrabo, comprobados contra `java` real.
//
// Todos comparten una propiedad, y es la que los hace implementables en KajiJDK: **la fuente o el
// destino los aporta quien llama**. Un canal ya abierto, un `OutputStream` ya construido, un
// `ClassLoader` que ya existe. Ninguno pide que la biblioteca sepa tocar el sistema de archivos, que
// es justamente lo que sigue faltando -- por eso `Scanner(File)` y `Formatter(File)` no estan.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.ServiceLoader;

public class IoBridgeTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    // Un canal sobre un arreglo de bytes. Es todo lo que hace falta para probar la forma: el canal
    // lo aporta quien llama, y esta clase es "quien llama".
    static class CanalDeBytes implements ReadableByteChannel {
        private final byte[] datos;
        private int pos = 0;
        private boolean abierto = true;
        private final int porVuelta;

        CanalDeBytes(byte[] datos, int porVuelta) {
            this.datos = datos;
            this.porVuelta = porVuelta;
        }

        public int read(ByteBuffer dst) throws IOException {
            if (pos >= datos.length) {
                return -1;                       // fin de flujo
            }
            int n = datos.length - pos;
            if (n > porVuelta) {
                n = porVuelta;
            }
            if (n > dst.remaining()) {
                n = dst.remaining();
            }
            dst.put(datos, pos, n);
            pos = pos + n;
            return n;
        }

        public boolean isOpen() {
            return abierto;
        }

        public void close() {
            abierto = false;
        }
    }

    static void canal() {
        byte[] datos = "uno dos 33 4.5\nsegunda linea".getBytes(StandardCharsets.UTF_8);

        Scanner s = new Scanner(new CanalDeBytes(datos, 8192), StandardCharsets.UTF_8);
        ok("uno".equals(s.next()));
        ok("dos".equals(s.next()));
        ok(s.nextInt() == 33);
        ok(s.nextDouble() == 4.5d);
        // Ojo: el primer `nextLine()` devuelve **el resto de la linea actual** --vacio, porque
        // `nextDouble` consumio el ultimo token-- y recien el segundo trae la siguiente. Lo tenia
        // mal y lo dijo `java` real.
        ok("".equals(s.nextLine()));
        ok("segunda linea".equals(s.nextLine()));

        // Con lecturas de a 3 bytes: el bucle tiene que seguir hasta el -1 y no cortar antes. Es la
        // parte que importa, porque un canal puede devolver menos de lo pedido en cada vuelta.
        Scanner t = new Scanner(new CanalDeBytes(datos, 3), StandardCharsets.UTF_8);
        ok("uno".equals(t.next()));
        ok("dos".equals(t.next()));
        ok(t.nextInt() == 33);

        // Y con el nombre del charset en vez del objeto.
        Scanner u = new Scanner(new CanalDeBytes(datos, 7), "UTF-8");
        ok("uno".equals(u.next()));

        // Un canal vacio: cero tokens, sin colgarse.
        Scanner v = new Scanner(new CanalDeBytes(new byte[0], 4), StandardCharsets.UTF_8);
        ok(!v.hasNext());

        boolean npe = false;
        try {
            new Scanner((ReadableByteChannel) null, StandardCharsets.UTF_8);
        } catch (NullPointerException e) {
            npe = true;
        }
        ok(npe);
    }

    static void formateador() throws java.io.UnsupportedEncodingException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Formatter f = new Formatter(bos, StandardCharsets.UTF_8, Locale.ROOT);
        f.format("%s=%d", "x", 42);
        f.format("|%05.2f", 3.5d);
        f.close();
        ok("x=42|03.50".equals(new String(bos.toByteArray(), StandardCharsets.UTF_8)));
        // Sin errores de escritura, ioException() es null.
        ok(f.ioException() == null);

        // Un caracter fuera del plano basico: son dos `char` que juntos dan cuatro bytes en UTF-8.
        // Si se codificara char por char, cada mitad daria el caracter de reemplazo.
        ByteArrayOutputStream b2 = new ByteArrayOutputStream();
        Formatter g = new Formatter(b2, StandardCharsets.UTF_8, Locale.ROOT);
        g.format("%s", "a😀b");
        g.close();
        byte[] salida = b2.toByteArray();
        ok(salida.length == 6);
        ok("a😀b".equals(new String(salida, StandardCharsets.UTF_8)));

        // El constructor con nombre de charset, y el de charset por defecto.
        ByteArrayOutputStream b3 = new ByteArrayOutputStream();
        Formatter h = new Formatter(b3, "UTF-8", Locale.ROOT);
        h.format("%s", "hola");
        h.close();
        ok("hola".equals(new String(b3.toByteArray(), StandardCharsets.UTF_8)));

        ByteArrayOutputStream b4 = new ByteArrayOutputStream();
        Formatter i = new Formatter(b4);
        i.format("%d", 7);
        i.close();
        ok(b4.toByteArray().length == 1);
    }

    static void modulos() {
        // Una capa vacia no tiene modulos, asi que no tiene proveedores. No es un stub: la regla del
        // JDK es que esta forma busca **solo** en los modulos de la capa, y un proveedor del
        // classpath no cuenta.
        ServiceLoader<CharSequence> sl = ServiceLoader.load(ModuleLayer.empty(), CharSequence.class);
        ok(sl != null);
        ok(!sl.iterator().hasNext());

        boolean npe = false;
        try {
            ServiceLoader.load((ModuleLayer) null, CharSequence.class);
        } catch (NullPointerException e) {
            npe = true;
        }
        ok(npe);

        // getBundle por modulo: un bundle que no existe tiene que dar MissingResourceException,
        // igual que las otras formas de getBundle.
        Module m = IoBridgeTest.class.getModule();
        boolean mre = false;
        try {
            ResourceBundle.getBundle("no.existe.este.Bundle", m);
        } catch (MissingResourceException e) {
            mre = true;
        }
        ok(mre);

        mre = false;
        try {
            ResourceBundle.getBundle("no.existe.este.Bundle", Locale.ROOT, m);
        } catch (MissingResourceException e) {
            mre = true;
        }
        ok(mre);
    }

    public static int run() throws java.io.UnsupportedEncodingException {
        canal();
        formateador();
        modulos();
        return primerFallo;
    }

    // Para el arnes de regresion, que corre la clase con el `java` real y compara la salida.
    public static void main(String[] args) throws java.io.UnsupportedEncodingException {
        System.out.println(run());
    }
}
