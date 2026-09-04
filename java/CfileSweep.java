import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.PoolEntry;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// El barrido: lee TODOS los `.class` de `KajiLibrary` y emite una linea por archivo con un resumen
// que las dos implementaciones tienen que dar igual. Es el mismo truco que `CfileOracle` pero sobre
// miles de archivos en vez de siete; el resumen es corto a proposito para que el diff sea legible.
//
// La linea es: ruta|nombre interno|version|banderas|constant_pool_count|suma de etiquetas|campos|
// metodos|cuantos metodos tienen `Code`|largo total de todos los `Code`.
//
// La suma de etiquetas es sum(indice * etiqueta) sobre las entradas vivas: cambia si una entrada
// cambia de tipo o si el lector se saltea una ranura, que son los dos errores que un lector de pool
// comete. No es un hash criptografico y no pretende serlo.
public class CfileSweep {

    static byte[] leer(String ruta) throws IOException {
        File f = new File(ruta);
        int largo = (int) f.length();
        byte[] datos = new byte[largo];
        FileInputStream in = new FileInputStream(f);
        try {
            int leidos = 0;
            while (leidos < largo) {
                int n = in.read(datos, leidos, largo - leidos);
                if (n <= 0) {
                    throw new IOException("se corto " + ruta + " en " + leidos + " de " + largo);
                }
                leidos += n;
            }
            return datos;
        } finally {
            in.close();
        }
    }

    static void juntar(File dir, List<String> out) {
        File[] hijos = dir.listFiles();
        if (hijos == null) {
            return;
        }
        for (int i = 0; i < hijos.length; i++) {
            File h = hijos[i];
            if (h.isDirectory()) {
                juntar(h, out);
            } else if (h.getName().endsWith(".class")) {
                out.add(h.getPath().replace('\\', '/'));
            }
        }
    }

    static String resumen(String ruta) {
        byte[] bytes;
        try {
            bytes = leer(ruta);
        } catch (IOException e) {
            return ruta + "|ERROR-IO";
        }
        ClassModel cm;
        try {
            cm = ClassFile.of().parse(bytes);
        } catch (RuntimeException e) {
            return ruta + "|RECHAZADO";
        }
        long suma = 0;
        int n = cm.constantPool().size();
        for (int i = 1; i < n; i++) {
            PoolEntry e;
            try {
                e = cm.constantPool().entryByIndex(i);
            } catch (RuntimeException ex) {
                continue;
            }
            suma += (long) i * e.tag();
        }
        List<FieldModel> campos = cm.fields();
        List<MethodModel> metodos = cm.methods();
        int conCodigo = 0;
        long instrucciones = 0;
        long bytesDeCodigo = 0;
        for (int i = 0; i < metodos.size(); i++) {
            MethodModel m = metodos.get(i);
            if (m.code().isPresent()) {
                conCodigo++;
                List<CodeElement> piezas = m.code().get().elementList();
                for (int j = 0; j < piezas.size(); j++) {
                    CodeElement ce = piezas.get(j);
                    // Solo las instrucciones: el JDK intercala pseudo-instrucciones (etiquetas,
                    // numeros de linea, variables locales) que dependen de que atributos se sepan
                    // leer, y eso no es lo que este barrido esta comparando.
                    if (ce instanceof Instruction) {
                        instrucciones++;
                        bytesDeCodigo += ((Instruction) ce).sizeInBytes();
                    }
                }
            }
        }
        return ruta + "|" + cm.thisClass().asInternalName()
                + "|" + cm.majorVersion() + "." + cm.minorVersion()
                + "|" + cm.flags().flagsMask()
                + "|" + n
                + "|" + suma
                + "|" + campos.size()
                + "|" + metodos.size()
                + "|" + conCodigo
                + "|" + instrucciones
                + "|" + bytesDeCodigo;
    }

    public static void main(String[] args) {
        List<String> rutas = new ArrayList<String>();
        juntar(new File("KajiLibrary"), rutas);
        String[] arr = rutas.toArray(new String[rutas.size()]);
        Arrays.sort(arr);
        for (int i = 0; i < arr.length; i++) {
            System.out.println(resumen(arr[i]));
        }
    }

    static int run() {
        main(new String[0]);
        return -1;
    }
}
