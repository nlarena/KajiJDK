// El acceso a archivos, de punta a punta: los nativos de la VM, `File`, `FileInputStream`,
// `FileOutputStream`, y `Scanner`/`Formatter` sobre ellos.
//
// **Se comprueba contra `java` real corriendo lo mismo**, y por eso la prueba crea sus propios
// archivos en vez de leer alguno del repo: los dos lados tienen que ver exactamente el mismo
// contenido, y un archivo del arbol podria diferir por finales de linea.
//
// Lo que mas se cuida es que las respuestas sean **verdaderas sobre el mundo**, que es lo que
// distingue esto de la version anterior: antes `exists()` devolvia `false` siempre, y un
// `new Scanner(archivo)` habria dicho "no existe" de un archivo que si esta. Aca se crea un archivo,
// se comprueba que se lo vea, se lo lee, y se comprueba que despues de borrarlo deje de verse.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Formatter;
import java.util.Locale;
import java.util.Scanner;

public class FileTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    static final String BASE = "_kaji_filetest_";

    static void archivo() throws Exception {
        File f = new File(BASE + "a.txt");
        // Estado inicial: no esta.
        if (f.exists()) {
            f.delete();
        }
        ok(!f.exists());
        ok(!f.isFile());
        ok(f.length() == 0L);

        // Crearlo y verlo.
        ok(f.createNewFile());
        ok(f.exists());
        ok(f.isFile());
        ok(!f.isDirectory());
        ok(f.length() == 0L);
        ok(!f.createNewFile());        // ya existe: false, y sin pisarlo

        // Escribir y comprobar el tamaño.
        FileOutputStream out = new FileOutputStream(f);
        out.write("hola".getBytes("UTF-8"));
        out.close();
        ok(f.length() == 4L);

        // Leer de vuelta.
        FileInputStream in = new FileInputStream(f);
        ok(in.available() == 4);
        byte[] buf = new byte[10];
        int n = in.read(buf, 0, 10);
        ok(n == 4);
        ok(buf[0] == (byte) 'h' && buf[3] == (byte) 'a');
        ok(in.read() == -1);           // fin de flujo
        ok(in.read(buf, 0, 10) == -1);
        in.close();

        // Append: se agrega, no se pisa.
        FileOutputStream out2 = new FileOutputStream(f, true);
        out2.write("!!".getBytes("UTF-8"));
        out2.close();
        ok(f.length() == 6L);

        // Y sin append se trunca **al construir**, aunque no se escriba nada.
        FileOutputStream out3 = new FileOutputStream(f);
        out3.close();
        ok(f.length() == 0L);

        // Borrarlo y comprobar que deja de verse.
        ok(f.delete());
        ok(!f.exists());
        ok(!f.delete());               // ya no esta

        // Un archivo que no existe se rechaza al abrirlo, con la excepcion que corresponde.
        boolean tiro = false;
        try {
            new FileInputStream(BASE + "no_existe.txt");
        } catch (FileNotFoundException e) {
            tiro = true;
        }
        ok(tiro);
    }

    static void directorio() throws Exception {
        File d = new File(BASE + "dir");
        if (d.exists()) {
            d.delete();
        }
        ok(d.mkdir());
        ok(d.exists());
        ok(d.isDirectory());
        ok(!d.isFile());

        // Abrir un directorio para leer falla, y con **su propio** mensaje: confundirlo con "no
        // existe" manda a buscar al lugar equivocado.
        boolean tiro = false;
        try {
            new FileInputStream(d);
        } catch (FileNotFoundException e) {
            tiro = true;
        }
        ok(tiro);

        ok(d.delete());
        ok(!d.exists());
    }

    static void escaner() throws Exception {
        File f = new File(BASE + "s.txt");
        FileOutputStream out = new FileOutputStream(f);
        out.write("uno dos 33 4.5\nsegunda linea".getBytes("UTF-8"));
        out.close();

        // La forma que antes era imposible: un Scanner sobre un archivo que existe de verdad.
        Scanner s = new Scanner(f, "UTF-8");
        ok("uno".equals(s.next()));
        ok("dos".equals(s.next()));
        ok(s.nextInt() == 33);
        ok(s.nextDouble() == 4.5d);
        ok("".equals(s.nextLine()));
        ok("segunda linea".equals(s.nextLine()));
        s.close();

        // Y por Path.
        Scanner t = new Scanner(f.toPath());
        ok("uno".equals(t.next()));
        t.close();

        // Un archivo que no esta: FileNotFoundException, no un Scanner vacio.
        boolean tiro = false;
        try {
            new Scanner(new File(BASE + "no_existe.txt"));
        } catch (FileNotFoundException e) {
            tiro = true;
        }
        ok(tiro);

        ok(f.delete());
    }

    static void formateador() throws Exception {
        File f = new File(BASE + "f.txt");
        Formatter fm = new Formatter(f, "UTF-8", Locale.ROOT);
        fm.format("%s=%d", "x", 42);
        fm.format("|%05.2f", 3.5d);
        fm.close();                    // sin esto se pierde: lo dice el javadoc de la clase

        ok(f.length() == 10L);         // "x=42|03.50"
        Scanner s = new Scanner(f, "UTF-8");
        ok("x=42|03.50".equals(s.nextLine()));
        s.close();

        // Por nombre de archivo.
        Formatter fm2 = new Formatter(BASE + "f2.txt");
        fm2.format("%d", 7);
        fm2.close();
        File f2 = new File(BASE + "f2.txt");
        ok(f2.length() == 1L);

        ok(f.delete());
        ok(f2.delete());
    }

    public static int run() throws Exception {
        archivo();
        directorio();
        escaner();
        formateador();
        return primerFallo;
    }

    // Para el arnes de regresion, que corre la clase con el `java` real y compara la salida.
    public static void main(String[] args) throws Exception {
        System.out.println(run());
    }
}
