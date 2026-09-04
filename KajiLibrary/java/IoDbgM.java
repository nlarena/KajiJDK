import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

// Prueba cruzada de verdad: un archivo escrito por una VM y leido por la otra. Las de IoDbgK
// comprueban el formato contra hexa fijo; esta comprueba que las dos VM se entienden sobre las
// **mismas clases**, que es lo unico que prueba que el formato sirve para lo que existe.
//
//   real:  java -cp /tmp/iom IoDbgM write     -> escribe kaji-cross-in.ser
//   kaji:  run-headless .../IoDbgM.class run  -> lee ese y escribe kaji-cross-out.ser
//   real:  java -cp /tmp/iom IoDbgM check     -> lee el que escribio kaji
public class IoDbgM {

    static class Punto implements Serializable {
        private static final long serialVersionUID = 1L;
        int x;
        int y;
    }

    static class Nodo implements Serializable {
        private static final long serialVersionUID = 2L;
        String nombre;
        Nodo otro;
    }

    static class Todos implements Serializable {
        private static final long serialVersionUID = 3L;
        boolean z;
        byte b;
        char c;
        short sh;
        int i;
        long j;
        float f;
        double d;
        String s;
        transient int noSale = 99;
    }

    static class ConEscritor implements Serializable {
        private static final long serialVersionUID = 4L;
        int n;
        int doble;
        String extra;
        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeInt(this.n * 2);
            out.writeUTF("extra");
        }
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            this.doble = in.readInt();
            this.extra = in.readUTF();
        }
    }

    /** Con `putFields`/`readFields`: los campos por nombre en los dos sentidos. */
    static class ConPut implements Serializable {
        private static final long serialVersionUID = 7L;
        int a;
        String b;
        private void writeObject(ObjectOutputStream out) throws IOException {
            ObjectOutputStream.PutField pf = out.putFields();
            pf.put("a", 42);
            pf.put("b", "puesto");
            out.writeFields();
        }
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            ObjectInputStream.GetField gf = in.readFields();
            this.a = gf.get("a", -1);
            this.b = (String) gf.get("b", null);
            // Un campo que no existe **ni en el flujo ni en la clase** no es "no vino": es que el
            // que llama se equivoco de nombre, y el contrato manda avisarselo en vez de devolverle
            // su propio valor por omision, que le esconderia el error para siempre.
            boolean aviso = false;
            try {
                gf.get("noExiste", 7);
            } catch (IllegalArgumentException esperada) {
                aviso = true;
            }
            // Los que si estan tienen que decir que **no** salieron por omision: vinieron del flujo.
            if (!aviso || gf.defaulted("a") || gf.defaulted("b")) {
                throw new IOException("readFields roto");
            }
        }
    }

    static class Padre implements Serializable {
        private static final long serialVersionUID = 5L;
        int arriba;
    }

    static class Hijo extends Padre implements Serializable {
        private static final long serialVersionUID = 6L;
        int abajo;
    }

    static class Externa implements java.io.Externalizable {
        private static final long serialVersionUID = 8L;
        int v;
        String t;
        public Externa() { }
        public void writeExternal(java.io.ObjectOutput out) throws IOException {
            out.writeInt(this.v);
            out.writeUTF("ext");
        }
        public void readExternal(java.io.ObjectInput in) throws IOException {
            this.v = in.readInt();
            this.t = in.readUTF();
        }
    }

    enum Color implements Serializable { ROJO, VERDE }

    private static File archivo(String nombre) {
        String t = System.getProperty("java.io.tmpdir");
        return new File(t == null ? "." : t, nombre);
    }

    /** El grafo que viaja. Los valores son fijos para que el que lee pueda comprobarlos. */
    private static Object[] grafo() {
        Punto p = new Punto();
        p.x = 3;
        p.y = -4;
        Nodo ciclo = new Nodo();
        ciclo.nombre = "a";
        ciclo.otro = ciclo;
        Todos t = new Todos();
        t.z = true;
        t.b = -2;
        t.c = 'Z';
        t.sh = -300;
        t.i = 70000;
        t.j = -5000000000L;
        t.f = 1.5F;
        t.d = -2.25D;
        t.s = "eñe";
        ConEscritor ce = new ConEscritor();
        ce.n = 5;
        Hijo h = new Hijo();
        h.arriba = 11;
        h.abajo = 22;
        Externa ex = new Externa();
        ex.v = 3;
        Nodo compartido = new Nodo();
        compartido.nombre = "uno";
        return new Object[] {
            p, ciclo, t, ce, h, ex, Color.VERDE, new ConPut(),
            new int[] { 1, 2, 3 }, new String[] { "a", "b" }, new double[] { 0.5D },
            "hola", null, compartido, compartido,
        };
    }

    private static void escribir(File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        Object[] g = grafo();
        for (int i = 0; i < g.length; i++) {
            oos.writeObject(g[i]);
        }
        oos.flush();
        oos.close();
    }

    /** -1 si todo dio, o el indice de lo primero que no. */
    private static int verificar(File f) throws Exception {
        FileInputStream fis = new FileInputStream(f);
        ObjectInputStream in = new ObjectInputStream(fis);
        int i = 0;
        Punto p = (Punto) in.readObject();
        if (p.x != 3 || p.y != -4) return i; i++;                          // 0
        Nodo ciclo = (Nodo) in.readObject();
        if (!ciclo.nombre.equals("a") || ciclo.otro != ciclo) return i; i++;  // 1
        Todos t = (Todos) in.readObject();
        if (!t.z || t.b != -2 || t.c != 'Z' || t.sh != -300) return i; i++;   // 2
        if (t.i != 70000 || t.j != -5000000000L) return i; i++;               // 3
        if (t.f != 1.5F || t.d != -2.25D) return i; i++;                      // 4
        if (!t.s.equals("eñe")) return i; i++;                           // 5
        // El transient no viaja, y el constructor no corre: queda en el cero de la VM.
        if (t.noSale != 0) return i; i++;                                     // 6
        ConEscritor ce = (ConEscritor) in.readObject();
        if (ce.n != 5 || ce.doble != 10 || !ce.extra.equals("extra")) return i; i++;  // 7
        Hijo h = (Hijo) in.readObject();
        if (h.arriba != 11 || h.abajo != 22) return i; i++;                   // 8
        Externa ex = (Externa) in.readObject();
        if (ex.v != 3 || !ex.t.equals("ext")) return i; i++;                  // 9
        if (in.readObject() != Color.VERDE) return i; i++;                    // 10
        ConPut cp = (ConPut) in.readObject();
        if (cp.a != 42 || !cp.b.equals("puesto")) return i; i++;              // 11
        int[] ai = (int[]) in.readObject();
        if (ai.length != 3 || ai[0] != 1 || ai[2] != 3) return i; i++;        // 12
        String[] as = (String[]) in.readObject();
        if (as.length != 2 || !as[0].equals("a") || !as[1].equals("b")) return i; i++;  // 13
        double[] ad = (double[]) in.readObject();
        if (ad.length != 1 || ad[0] != 0.5D) return i; i++;                   // 14
        if (!"hola".equals(in.readObject())) return i; i++;                   // 15
        if (in.readObject() != null) return i; i++;                           // 16
        Object c1 = in.readObject();
        Object c2 = in.readObject();
        if (c1 != c2) return i; i++;                                          // 17
        if (!((Nodo) c1).nombre.equals("uno")) return i; i++;                 // 18
        in.close();
        return -1;
    }

    public static int run() {
        try {
            int r = verificar(archivo("kaji-cross-in.ser"));
            System.out.println("leido del otro lado: " + r);
            if (r != -1) {
                return r;
            }
            escribir(archivo("kaji-cross-out.ser"));
            System.out.println("escrito kaji-cross-out.ser");
            return -1;
        } catch (Throwable e) {
            System.out.println("EXC " + e.getClass().getName() + ": " + e.getMessage());
            return 99;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("write")) {
            escribir(archivo("kaji-cross-in.ser"));
            System.out.println("escrito kaji-cross-in.ser");
            return;
        }
        if (args.length > 0 && args[0].equals("check")) {
            System.out.println("leido del otro lado: " + verificar(archivo("kaji-cross-out.ser")));
            return;
        }
        System.out.println(run());
    }
}
