import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

/**
 * `File.list()` y `listFiles()`, que hasta hace un rato devolvian `null` siempre.
 *
 * <p>No devolvian null por un error: no habia nativo que enumerara un directorio, y el `null` **es**
 * contrato ("no es un directorio o hubo una falla de E/S"). Lo que se levanto fue el sustrato --un
 * solo nativo, `Fs.list`-- y recien despues los cinco metodos.
 *
 * <p>Lo que se prueba es la distincion que da todo el valor: `null` es "no pude mirar" y un arreglo
 * de largo cero es "mire y esta vacio". Si las dos dieran lo mismo, un recorrido de arbol nunca se
 * enteraria de que le falto una rama.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25.
 */
public class FileListTest {

    static int fallas = 0;

    static void ok(String que, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + que);
            fallas = fallas + 1;
        }
    }

    public static int run() throws Exception {
        fallas = 0;
        File base = new File("filelist-tmp");
        FileListTest.borrarArbol(base);
        base.mkdir();
        new File(base, "uno.txt").createNewFile();
        new File(base, "dos.txt").createNewFile();
        new File(base, "tres.dat").createNewFile();
        File sub = new File(base, "sub");
        sub.mkdir();

        String[] nombres = base.list();
        ok("list no es null sobre un directorio", nombres != null);
        ok("list encuentra las cuatro entradas", nombres != null && nombres.length == 4);

        // La distincion que importa: vacio no es lo mismo que null.
        String[] deVacio = sub.list();
        ok("un directorio vacio da un arreglo de largo cero, no null",
                deVacio != null && deVacio.length == 0);
        File inexistente = new File(base, "no-existe");
        ok("un directorio que no existe da null", inexistente.list() == null);
        ok("y un archivo comun tambien da null", new File(base, "uno.txt").list() == null);

        // Los nombres son simples, sin la ruta adelante.
        boolean simples = true;
        for (String n : nombres) {
            if (n.indexOf('/') >= 0 || n.indexOf('\\') >= 0) {
                simples = false;
            }
        }
        ok("los nombres son simples", simples);

        // listFiles arma la ruta con este directorio de padre.
        File[] hijos = base.listFiles();
        ok("listFiles da lo mismo en cantidad", hijos != null && hijos.length == 4);
        boolean conPadre = true;
        for (File f : hijos) {
            if (!f.getPath().startsWith(base.getPath())) {
                conPadre = false;
            }
            if (!f.exists()) {
                conPadre = false;
            }
        }
        ok("cada hijo trae la ruta completa y existe", conPadre);

        // Los filtros.
        String[] soloTxt = base.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".txt");
            }
        });
        ok("el filtro de nombre deja dos", soloTxt != null && soloTxt.length == 2);

        File[] soloDirs = base.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });
        ok("el filtro de File deja solo el subdirectorio",
                soloDirs != null && soloDirs.length == 1 && "sub".equals(soloDirs[0].getName()));

        File[] txtComoFile = base.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".dat");
            }
        });
        ok("listFiles con filtro de nombre deja uno",
                txtComoFile != null && txtComoFile.length == 1);

        // Un filtro nulo no filtra nada, que es lo que el contrato dice.
        ok("filtro nulo devuelve todo", base.list(null) != null && base.list(null).length == 4);

        FileListTest.borrarArbol(base);
        if (fallas == 0) {
            return -1;
        }
        return fallas;
    }

    static void borrarArbol(File f) {
        File[] hijos = f.listFiles();
        if (hijos != null) {
            for (File h : hijos) {
                FileListTest.borrarArbol(h);
            }
        }
        f.delete();
    }

    public static void main(String[] a) throws Exception {
        System.out.println("FileListTest " + FileListTest.run());
    }
}
