import java.io.FileOutputStream;

/**
 * Vuelca a disco el `.class` que construye `CfBuildTest`.
 *
 * <p>Existe para la comprobacion que ninguna prueba puede hacer sola: que una JVM real cargue y
 * corra los bytes que nuestro escritor produjo. Nuestro lector leyendo a nuestro escritor puede
 * estar de acuerdo en un error; `java` de verdad no.
 */
public class CfDump {

    public static int run() throws Exception {
        byte[] bytes = CfBuildTest.construir();
        CfDump.escribir("scratchpad/zzbuild/Hecha.class", bytes);
        // Y la copia transformada: es el camino que de verdad ejercita la adopcion de entradas de
        // otro pool, que es donde estuvo el error.
        java.lang.classfile.ClassModel m = java.lang.classfile.ClassFile.of().parse(bytes);
        byte[] copia = java.lang.classfile.ClassFile.of().transformClass(m,
                java.lang.classfile.ClassTransform.ACCEPT_ALL);
        CfDump.escribir("scratchpad/zzbuild/copia/Hecha.class", copia);
        return -1;
    }

    static void escribir(String ruta, byte[] bytes) throws Exception {
        FileOutputStream out = new FileOutputStream(ruta);
        out.write(bytes);
        out.close();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("CfDump " + CfDump.run());
    }
}
