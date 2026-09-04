import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.security.PermissionCollection;
import java.util.function.Predicate;

/**
 * Prueba de comportamiento del cierre de `java.io`.
 *
 * <p>`run()` devuelve -1 si todo pasa, o el indice de la primera comprobacion que falla. Se corre en
 * las dos VMs --la nuestra y el JDK real-- y tienen que dar lo mismo: el valor no dice "anda", dice
 * "las dos hacen lo mismo", que es lo unico verificable sin un oraculo aparte.
 */
public class IoTest {

    private static int n;
    private static int fallo = -1;

    private static void ok(boolean b) {
        if (fallo < 0 && !b) {
            fallo = n;
        }
        n = n + 1;
    }

    private static void eq(Object a, Object b) {
        ok(a == null ? b == null : a.equals(b));
    }

    // Si `p` sobre `a` implica `p2` sobre `b`, con las acciones dadas.
    private static boolean imp(String a, String aa, String b, String ba) {
        return new FilePermission(a, aa).implies(new FilePermission(b, ba));
    }

    private static boolean impR(String a, String b) {
        return imp(a, "read", b, "read");
    }

    public static int run() {
        n = 0;
        fallo = -1;
        objectStreamField();
        filePermission();
        try {
            randomAccessFile();
        } catch (Throwable t) {
            // Una excepcion que se escapa cuenta como el fallo de la comprobacion en curso: si no,
            // el numero saldria igual en las dos VMs por casualidad de haberse cortado en el mismo
            // lugar, que es justo lo que la prueba no debe dejar pasar.
            ok(false);
        }
        objectInputFilter();
        objectStreamClass();
        return fallo;
    }

    // ---- ObjectInputFilter ---------------------------------------------------------------------

    // Una `FilterInfo` de mentira: lo unico que las combinaciones miran es la clase.
    private static ObjectInputFilter.FilterInfo info(final Class<?> c) {
        return new ObjectInputFilter.FilterInfo() {
            public Class<?> serialClass() {
                return c;
            }

            public long arrayLength() {
                return -1;
            }

            public long depth() {
                return 1;
            }

            public long references() {
                return 0;
            }

            public long streamBytes() {
                return 0;
            }
        };
    }

    private static ObjectInputFilter fijo(final ObjectInputFilter.Status s) {
        return new ObjectInputFilter() {
            public ObjectInputFilter.Status checkInput(ObjectInputFilter.FilterInfo i) {
                return s;
            }
        };
    }

    private static class EsString implements Predicate<Class<?>> {
        public boolean test(Class<?> c) {
            return c == String.class;
        }
    }

    private static void objectInputFilter() {
        ObjectInputFilter.Status al = ObjectInputFilter.Status.ALLOWED;
        ObjectInputFilter.Status re = ObjectInputFilter.Status.REJECTED;
        ObjectInputFilter.Status un = ObjectInputFilter.Status.UNDECIDED;

        Predicate<Class<?>> p = new EsString();
        ObjectInputFilter deja = ObjectInputFilter.allowFilter(p, re);
        ok(deja.checkInput(info(String.class)) == al);
        ok(deja.checkInput(info(Integer.class)) == re);
        ok(deja.checkInput(info(null)) == un);
        // El arreglo NO se desenvuelve: `String[]` no es `String`.
        ok(deja.checkInput(info(new String[0].getClass())) == re);
        ok(deja.checkInput(info(new int[0].getClass())) == re);

        ObjectInputFilter niega = ObjectInputFilter.rejectFilter(p, un);
        ok(niega.checkInput(info(String.class)) == re);
        ok(niega.checkInput(info(Integer.class)) == un);
        ok(niega.checkInput(info(null)) == un);

        // La tabla entera de `merge`: cualquier rechazo gana, si no cualquier permiso, si no nada.
        eq(tablaMerge(), "UU->U UA->A UR->R AU->A AA->A AR->R RU->R RA->R RR->R");

        // rejectUndecidedClass: solo convierte el indeciso, y solo cuando hay clase.
        ok(ObjectInputFilter.rejectUndecidedClass(fijo(un)).checkInput(info(String.class)) == re);
        ok(ObjectInputFilter.rejectUndecidedClass(fijo(un)).checkInput(info(null)) == un);
        ok(ObjectInputFilter.rejectUndecidedClass(fijo(al)).checkInput(info(String.class)) == al);
        ok(ObjectInputFilter.rejectUndecidedClass(fijo(re)).checkInput(info(String.class)) == re);

        // El enum y su orden, que es parte de la API.
        ok(ObjectInputFilter.Status.values().length == 3);
        eq(ObjectInputFilter.Status.values()[0].name(), "UNDECIDED");
        eq(ObjectInputFilter.Status.values()[1].name(), "ALLOWED");
        eq(ObjectInputFilter.Status.values()[2].name(), "REJECTED");
        ok(ObjectInputFilter.Status.valueOf("ALLOWED") == al);

        // Los nulos.
        ok(npeAllow(null, al));
        ok(npeAllow(p, null));
        ok(npeMerge());
        ok(npeRuc());
    }

    private static String tablaMerge() {
        ObjectInputFilter.Status[] todos = {ObjectInputFilter.Status.UNDECIDED,
                                            ObjectInputFilter.Status.ALLOWED,
                                            ObjectInputFilter.Status.REJECTED};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < todos.length; i++) {
            for (int j = 0; j < todos.length; j++) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(todos[i].name().charAt(0));
                sb.append(todos[j].name().charAt(0));
                sb.append("->");
                sb.append(ObjectInputFilter.merge(fijo(todos[i]), fijo(todos[j]))
                          .checkInput(info(String.class)).name().charAt(0));
            }
        }
        return sb.toString();
    }

    private static boolean npeAllow(Predicate<Class<?>> p, ObjectInputFilter.Status s) {
        try {
            ObjectInputFilter.allowFilter(p, s);
            return false;
        } catch (NullPointerException ex) {
            return true;
        }
    }

    private static boolean npeMerge() {
        try {
            ObjectInputFilter.merge(null, fijo(ObjectInputFilter.Status.ALLOWED));
            return false;
        } catch (NullPointerException ex) {
            return true;
        }
    }

    private static boolean npeRuc() {
        try {
            ObjectInputFilter.rejectUndecidedClass(null);
            return false;
        } catch (NullPointerException ex) {
            return true;
        }
    }

    // ---- ObjectStreamClass ---------------------------------------------------------------------

    static class Pelada {
        int a;
    }

    static class Guardable implements Serializable {
        int zeta;
        String alfa;
        static int estatico;
        transient long saltado;
        double beta;
        private int privado;
    }

    static class Hija extends Guardable {
        int propio;
    }

    static class Externa implements java.io.Externalizable {
        int x;

        public void writeExternal(java.io.ObjectOutput o) {
        }

        public void readExternal(java.io.ObjectInput i) {
        }
    }

    static class ConLista implements Serializable {
        int noSale;
        private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("uno", Integer.TYPE),
            new ObjectStreamField("dos", String.class)
        };
    }

    static class Anchos implements Serializable {
        boolean bo;
        byte by;
        char ch;
        short sh;
        int in;
        long lo;
        float fl;
        double db;
        String re;
        int[] ar;
    }

    private static void objectStreamClass() {
        ok(ObjectStreamClass.NO_FIELDS.length == 0);

        // Una clase que no es serializable no tiene descriptor.
        ok(ObjectStreamClass.lookup(Pelada.class) == null);
        ok(ObjectStreamClass.lookupAny(Pelada.class) != null);
        ok(ObjectStreamClass.lookupAny(Pelada.class).getFields().length == 0);
        eq(ObjectStreamClass.lookupAny(Pelada.class).getName(), Pelada.class.getName());
        ok(ObjectStreamClass.lookup(Integer.TYPE) == null);
        eq(ObjectStreamClass.lookupAny(Integer.TYPE).getName(), "int");

        ObjectStreamClass d = ObjectStreamClass.lookup(Guardable.class);
        eq(d.getName(), Guardable.class.getName());
        ok(d.forClass() == Guardable.class);
        // Primitivos por nombre, despues referencias por nombre; `static` y `transient` afuera.
        eq(campos(d), "D beta@0 | I privado@8 | I zeta@12 | Ljava/lang/String; alfa@0");
        eq(d.getField("alfa").toString(), "Ljava/lang/String; alfa");
        ok(d.getField("nope") == null);
        ok(d.getField("estatico") == null);
        ok(d.getField("saltado") == null);
        ok(d.getFields() != d.getFields());

        // Solo los campos propios: los heredados son del descriptor de la superclase.
        eq(campos(ObjectStreamClass.lookup(Hija.class)), "I propio@0");

        // Una externalizable escribe lo suyo: no declara campos.
        ok(ObjectStreamClass.lookup(Externa.class).getFields().length == 0);

        // `serialPersistentFields` manda sobre los campos reales.
        eq(campos(ObjectStreamClass.lookup(ConLista.class)), "I uno@0 | Ljava/lang/String; dos@0");

        // Los desplazamientos: bytes para los primitivos, indices para las referencias.
        eq(campos(ObjectStreamClass.lookup(Anchos.class)),
           "Z bo@0 | B by@1 | C ch@2 | D db@4 | F fl@12 | I in@16 | J lo@20 | S sh@28 "
           + "| [I ar@0 | Ljava/lang/String; re@1");

        // Arreglos e interfaces: serializables, sin campos.
        ok(ObjectStreamClass.lookup(new int[0].getClass()) != null);
        ok(ObjectStreamClass.lookup(new int[0].getClass()).getFields().length == 0);
        ok(ObjectStreamClass.lookup(Serializable.class) != null);

        ok(npeLookup(true));
        ok(npeLookup(false));
    }

    private static String campos(ObjectStreamClass d) {
        ObjectStreamField[] fs = d.getFields();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fs.length; i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(fs[i].toString());
            sb.append('@');
            sb.append(fs[i].getOffset());
        }
        return sb.toString();
    }

    private static boolean npeLookup(boolean cual) {
        try {
            if (cual) {
                ObjectStreamClass.lookup(null);
            } else {
                ObjectStreamClass.lookupAny(null);
            }
            return false;
        } catch (NullPointerException ex) {
            return true;
        }
    }

    // ---- ObjectStreamField ---------------------------------------------------------------------

    private static void objectStreamField() {
        ObjectStreamField i = new ObjectStreamField("x", Integer.TYPE);
        ok(i.getTypeCode() == 'I');
        ok(i.getTypeString() == null);
        ok(i.isPrimitive());
        ok(!i.isUnshared());
        ok(i.getOffset() == 0);
        eq(i.getName(), "x");
        ok(i.getType() == Integer.TYPE);
        eq(i.toString(), "I x");

        ObjectStreamField s = new ObjectStreamField("s", String.class);
        ok(s.getTypeCode() == 'L');
        eq(s.getTypeString(), "Ljava/lang/String;");
        ok(!s.isPrimitive());
        eq(s.toString(), "Ljava/lang/String; s");

        ObjectStreamField ai = new ObjectStreamField("a", new int[0].getClass());
        ok(ai.getTypeCode() == '[');
        eq(ai.getTypeString(), "[I");
        ok(!ai.isPrimitive());

        ObjectStreamField add = new ObjectStreamField("m", new double[0][0].getClass());
        eq(add.getTypeString(), "[[D");

        ObjectStreamField as = new ObjectStreamField("t", new String[0].getClass());
        eq(as.getTypeString(), "[Ljava/lang/String;");

        // Los ocho primitivos, para que una letra cambiada no pase.
        eq(letras(), "BCDFIJSZ");

        ObjectStreamField u = new ObjectStreamField("u", String.class, true);
        ok(u.isUnshared());

        // Orden: primitivo antes que referencia, y a igual categoria por nombre.
        ok(i.compareTo(s) < 0);
        ok(s.compareTo(i) > 0);
        ok(new ObjectStreamField("a", Integer.TYPE).compareTo(new ObjectStreamField("b", Long.TYPE)) < 0);
        ok(new ObjectStreamField("b", String.class).compareTo(new ObjectStreamField("a", String.class)) > 0);
        ok(new ObjectStreamField("a", Integer.TYPE).compareTo(new ObjectStreamField("a", Long.TYPE)) == 0);
    }

    private static String letras() {
        Class<?>[] cs = {Byte.TYPE, Character.TYPE, Double.TYPE, Float.TYPE,
                         Integer.TYPE, Long.TYPE, Short.TYPE, Boolean.TYPE};
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < cs.length; k++) {
            sb.append(new ObjectStreamField("f", cs[k]).getTypeCode());
        }
        return sb.toString();
    }

    // ---- FilePermission ------------------------------------------------------------------------

    private static void filePermission() {
        String bs = "\\";

        // Acciones: orden canonico, sin mayusculas, sin duplicados, sin espacios.
        eq(new FilePermission("/x", "write,read").getActions(), "read,write");
        eq(new FilePermission("/x", "delete,execute,readlink,write,read").getActions(),
           "read,write,execute,delete,readlink");
        eq(new FilePermission("/x", "READ").getActions(), "read");
        eq(new FilePermission("/x", "read,read,write").getActions(), "read,write");
        eq(new FilePermission("/x", " read , write ").getActions(), "read,write");

        // El nombre sale tal como se escribio, sin normalizar.
        eq(new FilePermission("/tmp/-", "read").getName(), "/tmp/-");
        eq(new FilePermission("C:/tmp/a", "read").getName(), "C:/tmp/a");

        // Comodin de un nivel.
        ok(impR("/tmp/*", "/tmp/a.txt"));
        ok(!impR("/tmp/*", "/tmp/sub/a.txt"));
        ok(!impR("/tmp/*", "/tmp"));
        ok(impR("/tmp/*", "/tmp/*"));
        ok(!impR("/tmp/*", "/tmp/-"));
        ok(!impR("/tmp/*", "/tmp/"));

        // Comodin recursivo.
        ok(impR("/tmp/-", "/tmp/sub/a.txt"));
        ok(!impR("/tmp/-", "/tmp"));
        ok(impR("/tmp/-", "/tmp/*"));
        ok(impR("/tmp/-", "/tmp/a"));

        // Sobre el directorio actual.
        ok(impR("*", "a.txt"));
        ok(!impR("*", "sub/a.txt"));
        ok(impR("-", "sub/a.txt"));

        // Ruta pelada: solo a si misma.
        ok(impR("/tmp/a", "/tmp/a"));
        ok(!impR("/tmp/a", "/tmp/b"));
        ok(impR("a.txt", "a.txt"));

        // Todo el sistema de archivos.
        ok(impR("<<ALL FILES>>", "/anything/x"));
        ok(impR("<<ALL FILES>>", "<<ALL FILES>>"));
        ok(!impR("/tmp/-", "<<ALL FILES>>"));

        // Normalizacion lexica: separadores y `.`.
        ok(impR("C:" + bs + "tmp" + bs + "*", "C:" + bs + "tmp" + bs + "a.txt"));
        ok(impR("C:/tmp/*", "C:" + bs + "tmp" + bs + "a.txt"));
        ok(impR("C:" + bs + "tmp" + bs + "*", "C:/tmp/a.txt"));
        ok(impR("a.txt", "./a.txt"));
        ok(impR("/tmp/./a", "/tmp/a"));
        ok(impR("*", "./a.txt"));

        // Y lo que NO se normaliza: relativo contra absoluto.
        ok(!impR("/tmp/a", "tmp/a"));

        // Acciones en `implies`.
        ok(imp("/tmp/-", "read,write", "/tmp/a", "read"));
        ok(!imp("/tmp/-", "read", "/tmp/a", "read,write"));
        ok(imp("/x", "read,write", "/x", "read"));
        ok(!imp("/x", "read", "/x", "read,write"));

        // Contra otra clase de permiso.
        ok(!new FilePermission("/x", "read").implies(new java.security.AllPermission()));

        // equals / hashCode.
        ok(new FilePermission("/tmp/a", "read,write").equals(new FilePermission("/tmp/a", "write,read")));
        ok(!new FilePermission("/tmp/a", "read").equals(new FilePermission("/tmp/b", "read")));
        ok(new FilePermission("/tmp/./a", "read").equals(new FilePermission("/tmp/a", "read")));
        ok(!new FilePermission("/tmp/a", "read").equals(new FilePermission("/tmp/*", "read")));
        // El contrato: iguales -> mismo hash. El valor exacto no esta especificado y no se compara.
        ok(new FilePermission("/tmp/./a", "read").hashCode()
           == new FilePermission("/tmp/a", "read").hashCode());
        ok(new FilePermission("/tmp/a", "read,write").hashCode()
           == new FilePermission("/tmp/a", "write,read").hashCode());

        // toString viene de Permission y nombra la clase, el nombre y las acciones.
        eq(new FilePermission("/tmp/a", "read").toString(),
           "(\"java.io.FilePermission\" \"/tmp/a\" \"read\")");

        // La coleccion.
        PermissionCollection pc = new FilePermission("/tmp/a", "read").newPermissionCollection();
        pc.add(new FilePermission("/tmp/-", "read"));
        ok(pc.implies(new FilePermission("/tmp/x/y", "read")));
        ok(!pc.implies(new FilePermission("/otro/y", "read")));
        ok(!pc.implies(new FilePermission("/tmp/x/y", "write")));
        int cuenta = 0;
        java.util.Enumeration<java.security.Permission> e = pc.elements();
        while (e.hasMoreElements()) {
            e.nextElement();
            cuenta = cuenta + 1;
        }
        ok(cuenta == 1);

        // Los rechazos del constructor.
        ok(tira("/x", "bogus"));
        ok(tira("/x", ""));
        ok(tira("/x", null));
        ok(tiraNPE(null, "read"));
    }

    private static boolean tira(String n2, String a) {
        try {
            new FilePermission(n2, a);
            return false;
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }

    private static boolean tiraNPE(String n2, String a) {
        try {
            new FilePermission(n2, a);
            return false;
        } catch (NullPointerException ex) {
            return true;
        }
    }

    // ---- RandomAccessFile ----------------------------------------------------------------------

    private static void randomAccessFile() throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir"), "kaji-iotest-raf");
        dir.mkdirs();
        File f = new File(dir, "t.bin");
        f.delete();

        // Los rechazos de apertura.
        ok(modoMalo(f, "q"));
        ok(modoMalo(f, "rwt"));
        ok(modoMalo(f, ""));
        ok(modoNulo(f));
        ok(noSeAbre(new File(dir, "no-esta-aca"), "r"));
        ok(noSeAbre(dir, "r"));

        // `rw` crea, y no trunca.
        RandomAccessFile w = new RandomAccessFile(f, "rw");
        ok(w.length() == 0);
        ok(w.getFilePointer() == 0);
        byte[] cinco = {1, 2, 3, 4, 5};
        w.write(cinco);
        ok(w.getFilePointer() == 5);
        ok(w.length() == 5);

        // Acceso aleatorio.
        w.seek(1);
        ok(w.read() == 2);
        ok(w.getFilePointer() == 2);

        // Mas alla del final: leer da -1 y el puntero no se mueve; escribir agranda con ceros.
        w.seek(10);
        ok(w.read() == -1);
        ok(w.getFilePointer() == 10);
        w.seek(10);
        w.write(0x7F);
        ok(w.length() == 11);
        w.seek(5);
        byte[] hueco = new byte[5];
        w.readFully(hueco);
        ok(ceros(hueco));

        // setLength: acortar recorta el puntero, agrandar no lo mueve.
        w.setLength(3);
        ok(w.length() == 3);
        ok(w.getFilePointer() == 3);
        w.setLength(8);
        ok(w.length() == 8);
        ok(w.getFilePointer() == 3);
        w.seek(0);
        byte[] todo = new byte[8];
        w.readFully(todo);
        eq(comoTexto(todo), "1,2,3,0,0,0,0,0");

        ok(tiraIO(w, 1));       // seek negativo
        ok(tiraIO(w, 2));       // setLength negativo

        // skipBytes se recorta al final y nunca es negativo.
        w.seek(6);
        ok(w.skipBytes(10) == 2);
        ok(w.getFilePointer() == 8);
        ok(w.skipBytes(-1) == 0);

        // Al final: -1 salvo que no se haya pedido nada.
        ok(w.read(new byte[4]) == -1);
        ok(w.read() == -1);
        ok(w.read(new byte[4], 0, 0) == 0);

        // El canal comparte la posicion: los dos sentidos.
        w.seek(2);
        ok(w.getChannel().position() == 2);
        w.getChannel().position(4);
        ok(w.getFilePointer() == 4);

        w.close();
        ok(tiraIO(w, 3));       // leer despues de cerrar
        w.close();              // cerrar dos veces no es un error
        ok(true);

        // Modo `r`: no deja escribir, y el rechazo es una IOException y no otra cosa.
        RandomAccessFile r = new RandomAccessFile(f, "r");
        ok(tiraIO(r, 4));
        ok(tiraIO(r, 5));
        ok(r.length() == 8);
        r.close();

        // Los primitivos, ida y vuelta.
        File g = new File(dir, "g.bin");
        RandomAccessFile x = new RandomAccessFile(g, "rw");
        x.setLength(0);
        x.seek(0);
        x.writeBoolean(true);
        x.writeByte(-2);
        x.writeShort(-300);
        x.writeChar('ñ');
        x.writeInt(-70000);
        x.writeLong(-5000000000L);
        x.writeFloat(1.5f);
        x.writeDouble(-2.25);
        x.writeUTF("holañ€");
        x.writeBytes("ABñ");
        x.writeChars("Zñ");
        ok(x.getFilePointer() == 48);
        x.seek(0);
        ok(x.readBoolean());
        ok(x.readByte() == -2);
        ok(x.readShort() == -300);
        ok(x.readChar() == 'ñ');
        ok(x.readInt() == -70000);
        ok(x.readLong() == -5000000000L);
        ok(x.readFloat() == 1.5f);
        ok(x.readDouble() == -2.25);
        eq(x.readUTF(), "holañ€");
        ok(x.readUnsignedByte() == 65);
        ok(x.readUnsignedByte() == 66);
        ok(x.readUnsignedByte() == 241);
        ok(x.readChar() == 'Z');
        ok(x.readChar() == 'ñ');
        ok(tiraIO(x, 6));       // readInt al final es EOFException
        x.close();

        // Sin signo.
        File z = new File(dir, "z.bin");
        RandomAccessFile u = new RandomAccessFile(z, "rw");
        u.setLength(0);
        u.seek(0);
        u.writeShort(0xFFFE);
        u.writeByte(0xFF);
        u.seek(0);
        ok(u.readUnsignedShort() == 65534);
        ok(u.readUnsignedByte() == 255);
        u.close();

        // readLine: los tres finales de linea, y null cuando ya no queda nada.
        File h = new File(dir, "h.txt");
        RandomAccessFile y = new RandomAccessFile(h, "rw");
        y.setLength(0);
        y.seek(0);
        y.writeBytes("uno\ndos\r\ntres\rcuatro");
        y.seek(0);
        eq(y.readLine(), "uno");
        eq(y.readLine(), "dos");
        eq(y.readLine(), "tres");
        eq(y.readLine(), "cuatro");
        eq(y.readLine(), null);
        y.close();

        f.delete();
        g.delete();
        z.delete();
        h.delete();
    }

    private static boolean ceros(byte[] b) {
        for (int k = 0; k < b.length; k++) {
            if (b[k] != 0) {
                return false;
            }
        }
        return true;
    }

    private static String comoTexto(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < b.length; k++) {
            if (k > 0) {
                sb.append(',');
            }
            sb.append(b[k]);
        }
        return sb.toString();
    }

    private static boolean modoMalo(File f, String m) {
        try {
            new RandomAccessFile(f, m).close();
            return false;
        } catch (IllegalArgumentException ex) {
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static boolean modoNulo(File f) {
        try {
            new RandomAccessFile(f, null).close();
            return false;
        } catch (NullPointerException ex) {
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static boolean noSeAbre(File f, String m) {
        try {
            new RandomAccessFile(f, m).close();
            return false;
        } catch (FileNotFoundException ex) {
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    // Cada `cual` es una operacion que tiene que salir por `IOException` y no por otra via. Van
    // juntas en un metodo porque lo que se comprueba es siempre lo mismo -- el **tipo** de la
    // excepcion -- y repetir el try/catch nueve veces solo agregaria lugares donde equivocarse.
    private static boolean tiraIO(RandomAccessFile a, int cual) {
        try {
            if (cual == 1) {
                a.seek(-1);
            } else if (cual == 2) {
                a.setLength(-1);
            } else if (cual == 3) {
                a.read();
            } else if (cual == 4) {
                a.write(1);
            } else if (cual == 5) {
                a.setLength(1);
            } else if (cual == 6) {
                a.readInt();
            }
            return false;
        } catch (IOException ex) {
            return true;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
