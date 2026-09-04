import java.lang.classfile.Attribute;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

// Vuelca los hechos verificables de un `.class` en un formato de una linea por hecho, para poder
// diffear nuestra lectura contra la del JDK 25. La MISMA fuente se compila con las dos cadenas:
// aca con `java.lang.classfile` nuestro, alla con el de `java.base`. Si una linea difiere, mentimos.
//
// Lo que se vuelca es lo que las dos implementaciones tienen que coincidir por contrato: nombre
// interno, superclase, interfaces, version, mascara de banderas, `constant_pool_count`, el histograma
// de etiquetas del pool, y nombre+descriptor+banderas+largo del `Code` de cada campo y metodo.
public class CfileOracle {

    static final String[] OBJETIVOS = {
        "KajiLibrary/java/lang/String.class",
        "KajiLibrary/java/util/ArrayList.class",
        "KajiLibrary/java/util/HashMap.class",
        "KajiLibrary/java/lang/Integer.class",
        "KajiLibrary/java/io/BufferedReader.class",
        "KajiLibrary/java/lang/classfile/Opcode.class",
        "KajiLibrary/jdk/internal/classfile/impl/ClassReaderImpl.class",
    };

    static byte[] leer(String ruta) throws IOException {
        FileInputStream in = new FileInputStream(ruta);
        try {
            byte[] buf = new byte[65536];
            byte[] acum = new byte[0];
            int n;
            while ((n = in.read(buf)) > 0) {
                byte[] nuevo = new byte[acum.length + n];
                System.arraycopy(acum, 0, nuevo, 0, acum.length);
                System.arraycopy(buf, 0, nuevo, acum.length, n);
                acum = nuevo;
            }
            return acum;
        } finally {
            in.close();
        }
    }

    static void volcar(String ruta) throws IOException {
        ClassModel cm = ClassFile.of().parse(leer(ruta));
        String p = ruta + "|";
        System.out.println(p + "this=" + cm.thisClass().asInternalName());
        System.out.println(p + "super=" + (cm.superclass().isPresent()
                ? cm.superclass().get().asInternalName() : "<none>"));
        List<ClassEntry> ifs = cm.interfaces();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ifs.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(ifs.get(i).asInternalName());
        }
        System.out.println(p + "ifaces=" + sb);
        System.out.println(p + "version=" + cm.majorVersion() + "." + cm.minorVersion());
        System.out.println(p + "flags=" + cm.flags().flagsMask());
        System.out.println(p + "poolsize=" + cm.constantPool().size());
        System.out.println(p + "bsmcount=" + cm.constantPool().bootstrapMethodCount());
        int[] hist = new int[24];
        for (int i = 1; i < cm.constantPool().size(); i++) {
            PoolEntry e;
            try {
                e = cm.constantPool().entryByIndex(i);
            } catch (RuntimeException ex) {
                continue; // ranura muerta detras de un long/double
            }
            hist[e.tag()]++;
        }
        StringBuilder hb = new StringBuilder();
        for (int t = 1; t < hist.length; t++) {
            if (hist[t] != 0) {
                hb.append(t).append(':').append(hist[t]).append(' ');
            }
        }
        System.out.println(p + "hist=" + hb.toString().trim());
        System.out.println(p + "attrs=" + nombresDeAtributos(cm.attributes()));
        List<FieldModel> campos = cm.fields();
        System.out.println(p + "nfields=" + campos.size());
        for (int i = 0; i < campos.size(); i++) {
            FieldModel f = campos.get(i);
            System.out.println(p + "field=" + f.fieldName().stringValue() + " "
                    + f.fieldType().stringValue() + " " + f.flags().flagsMask()
                    + " " + nombresDeAtributos(f.attributes()));
        }
        List<MethodModel> metodos = cm.methods();
        System.out.println(p + "nmethods=" + metodos.size());
        for (int i = 0; i < metodos.size(); i++) {
            MethodModel m = metodos.get(i);
            System.out.println(p + "method=" + m.methodName().stringValue() + " "
                    + m.methodType().stringValue() + " " + m.flags().flagsMask()
                    + " code=" + (m.code().isPresent() ? "si" : "no")
                    + " " + nombresDeAtributos(m.attributes()));
        }
    }

    static String nombresDeAtributos(List<Attribute<?>> as) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < as.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(as.get(i).attributeName().stringValue());
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < OBJETIVOS.length; i++) {
            volcar(OBJETIVOS[i]);
        }
    }

    static int run() {
        try {
            main(new String[0]);
            return -1;
        } catch (Exception e) {
            System.out.println("FALLO " + e);
            return 1;
        }
    }
}
