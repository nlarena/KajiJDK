import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.Serializable;

/**
 * Prueba de comportamiento de java.io, escrita para correr **igual** en esta VM y en el JDK real.
 *
 * <p>Cada comprobacion tiene un indice. {@code run()} devuelve -1 si pasaron todas, o el indice de
 * la primera que fallo: un solo int alcanza para comparar las dos VMs sin depender de que la salida
 * por consola coincida caracter por caracter.
 *
 * <p>Todo lo que toca el disco va al directorio temporal del sistema y se borra al salir. Ningun
 * caso escribe dentro del proyecto: una prueba que ensucia el arbol se vuelve imposible de repetir.
 */
public class IoTest {

    private static File tmpDir() {
        String t = System.getProperty("java.io.tmpdir");
        return new File(t == null ? "." : t);
    }

    public static int run() {
        int i = 0;
        File creado = null;
        try {
            // --- File.createTempFile ---
            creado = File.createTempFile("kaji", ".txt");
            if (!creado.exists()) return i; i++;                       // 0
            if (!creado.isFile()) return i; i++;                       // 1
            if (creado.length() != 0L) return i; i++;                  // 2
            if (!creado.getName().startsWith("kaji")) return i; i++;   // 3
            if (!creado.getName().endsWith(".txt")) return i; i++;     // 4
            // Dos llamadas seguidas no pueden dar el mismo archivo.
            File otro = File.createTempFile("kaji", ".txt");
            if (creado.equals(otro)) return i; i++;                    // 5
            otro.delete();

            // El sufijo nulo es `.tmp`, y un prefijo de menos de tres letras es ilegal.
            File sinSufijo = File.createTempFile("kaji", null);
            if (!sinSufijo.getName().endsWith(".tmp")) return i; i++;  // 6
            sinSufijo.delete();
            try {
                File.createTempFile("ab", ".txt");
                return i;                                             // 7
            } catch (IllegalArgumentException esperada) {
                i++;
            }

            // Con directorio explicito: el padre tiene que ser el que se pidio.
            File enDir = File.createTempFile("kaji", ".txt", tmpDir());
            if (!enDir.exists()) return i; i++;                        // 8
            enDir.delete();

            // --- lastModified / setLastModified ---
            // Multiplo de 1000: hay sistemas de archivos que truncan a segundos, y la prueba no
            // esta para medir la granularidad del disco.
            long cuando = 1234567000L;
            if (!creado.setLastModified(cuando)) return i; i++;        // 9
            if (creado.lastModified() != cuando) return i; i++;        // 10

            // Un archivo que no existe no tiene fecha: cero, que es lo que dice el contrato. Y el
            // cero **no** significa "1 de enero de 1970": para eso esta el caso de abajo.
            File ausente = new File(tmpDir(), "kaji-no-existe-nunca-jamas");
            ausente.delete();
            if (ausente.lastModified() != 0L) return i; i++;           // 11
            if (ausente.setLastModified(cuando)) return i; i++;        // 12

            // La epoca es una fecha valida y se distingue de "no se sabe" porque el archivo existe.
            if (!creado.setLastModified(0L)) return i; i++;            // 13
            if (creado.lastModified() != 0L) return i; i++;            // 14
            if (!creado.exists()) return i; i++;                       // 15
            creado.setLastModified(cuando);

            // Una fecha negativa es ilegal, no un `false`.
            try {
                creado.setLastModified(-1L);
                return i;                                             // 16
            } catch (IllegalArgumentException esperada) {
                i++;
            }

            // --- getCanonicalPath ---
            String canon = creado.getCanonicalPath();
            // Nada de rutas "verbatim" de Windows: el JDK devuelve `C:\...`, no `\\?\C:\...`.
            if (canon.startsWith("\\\\?\\")) return i; i++;            // 17
            if (!new File(canon).exists()) return i; i++;              // 18
            if (!new File(canon).isAbsolute()) return i; i++;          // 19
            // Canonicalizar dos veces da lo mismo: es un punto fijo.
            if (!new File(canon).getCanonicalPath().equals(canon)) return i; i++;  // 20

            // Los `.` y `..` se resuelven.
            File conPunto = new File(creado.getParent() + File.separator + "."
                    + File.separator + creado.getName());
            if (!conPunto.getCanonicalPath().equals(canon)) return i; i++;         // 21

            // Un archivo que no existe igual tiene camino canonico: es una operacion sobre el
            // nombre, no sobre el contenido.
            String canonAusente = ausente.getCanonicalPath();
            if (!new File(canonAusente).isAbsolute()) return i; i++;   // 22
            if (canonAusente.startsWith("\\\\?\\")) return i; i++;     // 23
            if (!canonAusente.endsWith(ausente.getName())) return i; i++;          // 24

            // Una ruta relativa se canonicaliza contra el directorio de trabajo.
            String canonRel = new File("IoTest-relativo-inexistente").getCanonicalPath();
            if (!new File(canonRel).isAbsolute()) return i; i++;       // 25

            // --- getCanonicalFile coincide con getCanonicalPath ---
            if (!creado.getCanonicalFile().getPath().equals(canon)) return i; i++; // 26

            // Cada bloque siguiente numera desde una centena propia, para que agregar un caso en
            // uno no corra los indices de los demas y una diferencia entre las dos VMs siga
            // apuntando al mismo caso de una corrida a la otra.
            int r = streams();
            if (r >= 0) return r;
            r = tuberias();
            if (r >= 0) return r;
            r = uids();
            if (r >= 0) return r;
            r = serializa();
            if (r >= 0) return r;
            r = deserializa();
            if (r >= 0) return r;
            r = canales();
            if (r >= 0) return r;

            return -1;
        } catch (IOException e) {
            return i;
        } finally {
            if (creado != null) {
                creado.delete();
            }
        }
    }

    /**
     * Streams de archivo: lo que pasa **despues** de cerrar.
     *
     * <p>Todo esto mide una sola cosa: que el error salga como {@link IOException} chequeada y no
     * como una `RuntimeException`. Es la diferencia entre que un `catch (IOException e)` del que
     * llama agarre el error o lo deje pasar de largo hasta matar el hilo.
     */
    private static int streams() {
        int i = 100;
        File f = null;
        try {
            f = File.createTempFile("kaji", ".bin");

            java.io.FileOutputStream out = new java.io.FileOutputStream(f);
            out.write(65);
            out.close();
            // Escribir sobre un stream cerrado es una IOException, no una RuntimeException.
            try {
                out.write(66);
                return i;                                              // 100
            } catch (IOException esperada) {
                i++;
            }

            java.io.FileInputStream in = new java.io.FileInputStream(f);
            if (in.read() != 65) return i; i++;                        // 101
            if (in.read() != -1) return i; i++;                        // 102
            in.close();
            try {
                in.read();
                return i;                                             // 103
            } catch (IOException esperada) {
                i++;
            }

            // `reset()` sin marca valida tambien es IOException.
            java.io.FileInputStream in2 = new java.io.FileInputStream(f);
            try {
                in2.reset();
                return i;                                             // 104
            } catch (IOException esperada) {
                i++;
            } finally {
                in2.close();
            }

            // Y `available()` sobre un stream cerrado, idem.
            java.io.FileInputStream in3 = new java.io.FileInputStream(f);
            in3.close();
            try {
                in3.available();
                return i;                                             // 105
            } catch (IOException esperada) {
                i++;
            }

            return -1;
        } catch (IOException e) {
            return i;
        } finally {
            if (f != null) {
                f.delete();
            }
        }
    }

    /**
     * Tuberias: leer de una a la que se le murio el escritor.
     *
     * <p>Mismo punto que arriba. El JDK dice "Write end dead" con una {@link IOException}; si sale
     * envuelta en una no chequeada, el lazo de lectura de cualquiera que use una tuberia se cae en
     * vez de terminar.
     */
    private static int tuberias() {
        int i = 200;
        try {
            java.io.PipedOutputStream po = new java.io.PipedOutputStream();
            java.io.PipedInputStream pi = new java.io.PipedInputStream(po);
            po.write(7);
            if (pi.read() != 7) return i; i++;                        // 200
            pi.close();
            // Escribir en una tuberia con el lector cerrado: IOException.
            try {
                po.write(8);
                return i;                                             // 201
            } catch (IOException esperada) {
                i++;
            }

            java.io.PipedWriter pw = new java.io.PipedWriter();
            java.io.PipedReader pr = new java.io.PipedReader(pw);
            pw.write('x');
            if (pr.read() != 'x') return i; i++;                      // 202
            pr.close();
            try {
                pw.write('y');
                return i;                                             // 203
            } catch (IOException esperada) {
                i++;
            }

            // Conectar dos veces es IOException, no una no chequeada.
            java.io.PipedOutputStream po2 = new java.io.PipedOutputStream();
            java.io.PipedInputStream pi2 = new java.io.PipedInputStream(po2);
            try {
                po2.connect(new java.io.PipedInputStream());
                return i;                                             // 204
            } catch (IOException esperada) {
                i++;
            }
            pi2.close();
            po2.close();

            return -1;
        } catch (IOException e) {
            return i;
        }
    }

    // ---- serialVersionUID (300) ------------------------------------------------------------------
    //
    // Los numeros esperados salen del **JDK real** y no de esta implementacion: el UID no tiene un
    // valor "razonable" que se pueda razonar, su unica definicion es "lo que calcula el otro lado".
    // Cinco formas distintas, elegidas por lo que cada una mete en la huella: sin nada especial, con
    // el UID declarado a mano, con inicializador estatico --el dato que no da la reflexion--, una
    // interfaz, y una con miembros privados que **no** entran.

    /** Lo minimo: dos campos, sin `<clinit>` y sin UID declarado. */
    static class Simple implements Serializable {
        int x;
        String s;
    }

    /** Con el UID a mano: el calculo no corre y sale este numero tal cual. */
    static class ConSuid implements Serializable {
        private static final long serialVersionUID = 4242424242L;
        int x;
        String s;
        public int mas(int n) { return this.x + n; }
    }

    /**
     * Con inicializador estatico. Es el caso que separa una implementacion honesta de una que
     * adivina: el arreglo estatico genera un `<clinit>`, que entra en la huella y que
     * `getDeclaredMethods` no muestra.
     */
    static class ConClinit implements Serializable {
        static final int[] TABLA = new int[3];
        int x;
    }

    /** Una interfaz: modificadores propios y el ABSTRACT que depende de si tiene metodos. */
    interface Marca extends Serializable {
        int cuantos();
    }

    /**
     * Con miembros privados. Un `private static` y un `private transient` quedan afuera de la
     * huella; un `private` de instancia comun entra. Los metodos `private` quedan afuera.
     */
    static class ConPrivados implements Serializable {
        private static int contador;
        private transient Object cache;
        private int oculto;
        public String visible;
        protected long protegido;
        private void interno() { this.oculto = 1; }
        public int leer() { this.interno(); return this.oculto; }
    }

    private static int uids() {
        int i = 300;
        if (ObjectStreamClass.lookup(Simple.class).getSerialVersionUID()
                != 1989406208694753721L) return i; i++;              // 300
        if (ObjectStreamClass.lookup(ConSuid.class).getSerialVersionUID()
                != 4242424242L) return i; i++;                        // 301
        if (ObjectStreamClass.lookup(ConClinit.class).getSerialVersionUID()
                != 6345415093954581717L) return i; i++;               // 302
        if (ObjectStreamClass.lookup(Marca.class).getSerialVersionUID()
                != 1658330474448090110L) return i; i++;               // 303
        if (ObjectStreamClass.lookup(ConPrivados.class).getSerialVersionUID()
                != -1647293024607408705L) return i; i++;              // 304

        // El mismo descriptor pedido dos veces da el mismo numero: el cache no puede cambiar la
        // respuesta, y un UID que se mueve entre llamadas es peor que uno equivocado.
        ObjectStreamClass d = ObjectStreamClass.lookup(Simple.class);
        if (d.getSerialVersionUID() != d.getSerialVersionUID()) return i; i++;  // 305

        // Un arreglo **si** tiene UID calculado, y no cero: es serializable, y su huella sale del
        // nombre `[I` mas `Cloneable` y `Serializable`. Sorprende, y por eso esta el caso.
        if (ObjectStreamClass.lookup(int[].class).getSerialVersionUID()
                != 5600894804908749477L) return i; i++;               // 306

        // El `toString` del JDK es la linea de la declaracion, no solo el nombre. Sobre una clase
        // propia y no sobre `String`: en esta biblioteca `String` todavia no implementa
        // `Serializable`, asi que `lookup` da `null` -- lo cual es correcto para lo que String es
        // aca, y no lo que la prueba quiere medir.
        if (!ObjectStreamClass.lookup(ConSuid.class).toString().equals(
                "IoTest$ConSuid: static final long serialVersionUID = 4242424242L;"))
            return i; i++;                                            // 307

        return -1;
    }


    // ---- serializacion, byte por byte (400) ------------------------------------------------------
    //
    // Las cadenas esperadas son el **flujo que produce el JDK real** para el mismo objeto, en hexa.
    // Es la unica prueba que sirve para un formato de intercambio: "se puede volver a leer" lo cumple
    // cualquier formato inventado, y lo que hace falta es que lo lea la otra JVM.

    static class Punto implements Serializable {
        private static final long serialVersionUID = 1L;
        int x;
        int y;
        Punto(int x, int y) { this.x = x; this.y = y; }
    }

    /** Con referencia compartida y con ciclo: prueba la tabla de manijas. */
    static class Nodo implements Serializable {
        private static final long serialVersionUID = 2L;
        String nombre;
        Nodo otro;
        Nodo(String n) { this.nombre = n; }
    }

    /** Con todos los primitivos, para el orden y el empaquetado de los campos. */
    static class Todos implements Serializable {
        private static final long serialVersionUID = 3L;
        boolean z = true;
        byte b = -2;
        char c = 'Z';
        short sh = -300;
        int i = 70000;
        long j = -5000000000L;
        float f = 1.5F;
        double d = -2.25D;
        String s = "eñe";
        transient int noSale = 99;
    }

    /** Con `writeObject` propio: modo bloque y TC_ENDBLOCKDATA. */
    static class ConEscritor implements Serializable {
        private static final long serialVersionUID = 4L;
        int n = 5;
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeInt(this.n * 2);
            out.writeUTF("extra");
        }
    }

    /** Con `putFields`: los mismos bytes que la escritura por defecto, elegidos a mano. */
    static class ConPut implements Serializable {
        private static final long serialVersionUID = 7L;
        int a;
        String b;
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            java.io.ObjectOutputStream.PutField pf = out.putFields();
            pf.put("a", 42);
            pf.put("b", "puesto");
            out.writeFields();
        }
    }

    static class Padre implements Serializable {
        private static final long serialVersionUID = 5L;
        int arriba = 11;
    }

    /** Subclase: dos descriptores encadenados y los datos de arriba hacia abajo. */
    static class Hijo extends Padre implements Serializable {
        private static final long serialVersionUID = 6L;
        int abajo = 22;
    }

    static class Externa implements java.io.Externalizable {
        private static final long serialVersionUID = 8L;
        int v = 3;
        public Externa() { }
        public void writeExternal(java.io.ObjectOutput out) throws IOException {
            out.writeInt(this.v);
            out.writeUTF("ext");
        }
        public void readExternal(java.io.ObjectInput in) throws IOException {
            this.v = in.readInt();
            in.readUTF();
        }
    }

    enum Color implements Serializable { ROJO, VERDE }

    private static final char[] HEXA = "0123456789abcdef".toCharArray();

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEXA[(b[i] >> 4) & 0xF]).append(HEXA[b[i] & 0xF]);
        }
        return sb.toString();
    }

    /** Serializa `o` y devuelve el flujo entero en hexa. */
    private static String ser(Object o) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.flush();
        return hex(bos.toByteArray());
    }

    /** El caso de la referencia compartida: el mismo `Nodo` dos veces en el mismo flujo. */
    private static String serCompartido() throws IOException {
        Nodo n = new Nodo("uno");
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
        oos.writeObject(n);
        oos.writeObject(n);
        oos.flush();
        return hex(bos.toByteArray());
    }

    private static String[] casos() throws IOException {
        Nodo ciclo = new Nodo("a");
        ciclo.otro = ciclo;
        return new String[] {
            ser(null),
            ser("hola"),
            ser(new Punto(3, -4)),
            ser(new int[] { 1, 2, 3 }),
            ser(new String[] { "a", "b" }),
            ser(new Todos()),
            ser(new ConEscritor()),
            ser(new Hijo()),
            ser(new Externa()),
            ser(Color.VERDE),
            ser(ciclo),
            serCompartido(),
            ser(new ConPut()),
            ser(new byte[] { 10, -1 }),
            ser(new double[] { 0.5D }),
            ser(Punto.class),
        };
    }

    private static final String[] ESPERADO = {
        "aced000570",
        "aced0005740004686f6c61",
        "aced00057372000c496f546573742450756e746f00000000000000010200024900017849000179787000000003fffffffc",
        "aced0005757200025b494dba602676eab2a5020000787000000003000000010000000200000003",
        "aced0005757200135b4c6a6176612e6c616e672e537472696e673badd256e7e91d7b470200007870000000027400016174000162",
        "aced00057372000c496f5465737424546f646f73000000000000000302000942000162430001634400016446000166490001694a00016a53000273685a00017a4c0001737400124c6a6176612f6c616e672f537472696e673b7870fe005ac0020000000000003fc0000000011170fffffffed5fa0e00fed40174000465c3b165",
        "aced000573720012496f5465737424436f6e4573637269746f7200000000000000040300014900016e787000000005770b0000000a0005657874726178",
        "aced00057372000b496f546573742448696a6f00000000000000060200014900056162616a6f7872000c496f54657374245061647265000000000000000502000149000661727269626178700000000b00000016",
        "aced00057372000e496f546573742445787465726e6100000000000000080c00007870770900000003000365787478",
        "aced00057e72000c496f5465737424436f6c6f7200000000000000001200007872000e6a6176612e6c616e672e456e756d000000000000000012000078707400055645524445",
        "aced00057372000b496f54657374244e6f646f00000000000000020200024c00066e6f6d6272657400124c6a6176612f6c616e672f537472696e673b4c00046f74726f74000d4c496f54657374244e6f646f3b78707400016171007e0003",
        "aced00057372000b496f54657374244e6f646f00000000000000020200024c00066e6f6d6272657400124c6a6176612f6c616e672f537472696e673b4c00046f74726f74000d4c496f54657374244e6f646f3b7870740003756e6f7071007e0003",
        "aced00057372000d496f5465737424436f6e5075740000000000000007030002490001614c0001627400124c6a6176612f6c616e672f537472696e673b78700000002a74000670756573746f78",
        "aced0005757200025b42acf317f8060854e00200007870000000020aff",
        "aced0005757200025b443ea68c14ab635a1e0200007870000000013fe0000000000000",
        "aced00057672000c496f546573742450756e746f000000000000000102000249000178490001797870",
    };

    /**
     * Imprime los flujos de {@link #casos()} en hexa, uno por linea.
     *
     * <p>Es de donde salen las cadenas de {@code ESPERADO}: se corre contra el **JDK real** y se
     * pega el resultado. Tambien sirve para ver cual byte difiere cuando un caso falla.
     */
    public static int dump() {
        try {
            String[] h = casos();
            for (int k = 0; k < h.length; k++) System.out.println(h[k]);
        } catch (IOException e) {
            System.out.println("ERR " + e);
        }
        return -1;
    }

    private static int serializa() {
        int i = 400;
        try {
            String[] hechos = casos();
            for (int k = 0; k < ESPERADO.length; k++) {
                if (!hechos[k].equals(ESPERADO[k])) return i + k;   // 400..415
            }
            i = i + ESPERADO.length;

            // Un flujo vacio es solo la cabecera: cuatro bytes y nada mas.
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
            oos.flush();
            if (!hex(bos.toByteArray()).equals("aced0005")) return i; i++;   // 416

            // Lo que no es serializable no sale, y avisa con la excepcion que corresponde.
            try {
                ser(new Object());
                return i;                                                    // 417
            } catch (java.io.NotSerializableException esperada) {
                i++;
            }
            return -1;
        } catch (IOException e) {
            return i;
        }
    }


    // ---- deserializacion (500) --------------------------------------------------------------------
    //
    // El espejo del bloque de arriba, y la unica prueba que sirve para el lado que lee: las entradas
    // son **los mismos flujos que produjo el JDK real** --las cadenas de `ESPERADO`, sin tocar-- y lo
    // que se comprueba es que de ahi salgan los objetos que el JDK saca. "Lee lo que yo escribi" no
    // probaria nada: dos rutinas que se equivocan igual tambien lo cumplen.

    /** Una clase que lee lo suyo: `defaultReadObject` y despues los datos que agrego el escritor. */
    static class Ronda implements Serializable {
        private static final long serialVersionUID = 20L;
        int n = 5;
        transient int doble;
        transient String extra;
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeInt(this.n * 2);
            out.writeUTF("mas");
        }
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            this.doble = in.readInt();
            this.extra = in.readUTF();
        }
    }

    /** Los campos por nombre en los dos sentidos: `putFields` al escribir, `readFields` al leer. */
    static class Nombrados implements Serializable {
        private static final long serialVersionUID = 21L;
        int a;
        String b;
        transient boolean avisoDeInexistente;
        transient boolean porOmision;
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            java.io.ObjectOutputStream.PutField pf = out.putFields();
            pf.put("a", 9);
            pf.put("b", "nueve");
            out.writeFields();
        }
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            java.io.ObjectInputStream.GetField gf = in.readFields();
            this.a = gf.get("a", -1);
            this.b = (String) gf.get("b", null);
            // Un nombre que no existe ni en el flujo ni en la clase es un error del que llama, no un
            // campo ausente: tiene que avisar en vez de devolverle su propio valor por omision.
            try {
                gf.get("nadaQueVer", 7);
            } catch (IllegalArgumentException esperada) {
                this.avisoDeInexistente = true;
            }
            this.porOmision = gf.defaulted("a");
        }
    }

    /** Registra una validacion, que solo puede correr con el grafo entero armado. */
    static class Validada implements Serializable {
        private static final long serialVersionUID = 22L;
        static int validadas;
        int v = 1;
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            in.registerValidation(new Testigo(), 0);
        }
    }

    static class Testigo implements java.io.ObjectInputValidation {
        public void validateObject() {
            Validada.validadas = Validada.validadas + 1;
        }
    }

    /** Un filtro que rechaza una clase y no opina del resto. */
    static class Prohibe implements java.io.ObjectInputFilter {
        private final Class<?> vetada;
        Prohibe(Class<?> vetada) { this.vetada = vetada; }
        public java.io.ObjectInputFilter.Status checkInput(java.io.ObjectInputFilter.FilterInfo info) {
            if (info.serialClass() == this.vetada) {
                return java.io.ObjectInputFilter.Status.REJECTED;
            }
            return java.io.ObjectInputFilter.Status.UNDECIDED;
        }
    }

    private static byte[] bin(String h) {
        byte[] b = new byte[h.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int alto = Character.digit(h.charAt(i * 2), 16);
            int bajo = Character.digit(h.charAt(i * 2 + 1), 16);
            b[i] = (byte) ((alto << 4) | bajo);
        }
        return b;
    }

    private static java.io.ObjectInputStream flujo(byte[] b) throws IOException {
        return new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(b));
    }

    /** Lee el unico objeto de `hex`, que es un flujo escrito por el JDK real. */
    private static Object des(String hex) throws Exception {
        return flujo(bin(hex)).readObject();
    }

    /** Escribe `o` y lo vuelve a leer en esta misma VM. */
    private static Object vuelta(Object o) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.flush();
        return flujo(bos.toByteArray()).readObject();
    }

    private static int deserializa() {
        int i = 500;
        try {
            // --- los flujos del JDK real, leidos aca ---
            if (des(ESPERADO[0]) != null) return i; i++;                       // 500
            if (!"hola".equals(des(ESPERADO[1]))) return i; i++;               // 501
            Punto p = (Punto) des(ESPERADO[2]);
            if (p.x != 3 || p.y != -4) return i; i++;                          // 502
            int[] ai = (int[]) des(ESPERADO[3]);
            if (ai.length != 3 || ai[0] != 1 || ai[1] != 2 || ai[2] != 3) return i; i++;  // 503
            String[] as = (String[]) des(ESPERADO[4]);
            if (as.length != 2 || !as[0].equals("a") || !as[1].equals("b")) return i; i++;  // 504
            Todos t = (Todos) des(ESPERADO[5]);
            if (!t.z || t.b != -2 || t.c != 'Z' || t.sh != -300) return i; i++;   // 505
            if (t.i != 70000 || t.j != -5000000000L) return i; i++;               // 506
            if (t.f != 1.5F || t.d != -2.25D || !t.s.equals("e\u00f1e")) return i; i++;  // 507
            // El `transient` no viaja **y el constructor no corre**: queda en el cero de la VM, no
            // en el 99 que le pone el inicializador de campo.
            if (t.noSale != 0) return i; i++;                                     // 508
            // Escrito con `writeObject` propio y leido **sin** `readObject`: los campos por defecto
            // salen, y lo que el escritor agrego se saltea por el marco de bloque sin entenderlo.
            ConEscritor ce = (ConEscritor) des(ESPERADO[6]);
            if (ce.n != 5) return i; i++;                                         // 509
            Hijo h = (Hijo) des(ESPERADO[7]);
            if (h.arriba != 11 || h.abajo != 22) return i; i++;                   // 510
            Externa ex = (Externa) des(ESPERADO[8]);
            if (ex.v != 3) return i; i++;                                         // 511
            // La constante se busca por nombre: tiene que salir **la misma instancia**, no una copia.
            if (des(ESPERADO[9]) != Color.VERDE) return i; i++;                    // 512
            Nodo ciclo = (Nodo) des(ESPERADO[10]);
            if (!ciclo.nombre.equals("a")) return i; i++;                          // 513
            if (ciclo.otro != ciclo) return i; i++;                                // 514
            // Dos `writeObject` del mismo objeto: del otro lado tiene que haber **uno**.
            java.io.ObjectInputStream dos = flujo(bin(ESPERADO[11]));
            Object c1 = dos.readObject();
            Object c2 = dos.readObject();
            if (c1 != c2) return i; i++;                                           // 515
            if (!((Nodo) c1).nombre.equals("uno")) return i; i++;                  // 516
            ConPut cp = (ConPut) des(ESPERADO[12]);
            if (cp.a != 42 || !cp.b.equals("puesto")) return i; i++;               // 517
            byte[] ab = (byte[]) des(ESPERADO[13]);
            if (ab.length != 2 || ab[0] != 10 || ab[1] != -1) return i; i++;       // 518
            double[] ad = (double[]) des(ESPERADO[14]);
            if (ad.length != 1 || ad[0] != 0.5D) return i; i++;                    // 519
            if (des(ESPERADO[15]) != Punto.class) return i; i++;                    // 520

            // --- lo que solo se ve leyendo ---
            Ronda r = (Ronda) vuelta(new Ronda());
            if (r.n != 5 || r.doble != 10 || !"mas".equals(r.extra)) return i; i++;  // 521

            Nombrados nb = (Nombrados) vuelta(new Nombrados());
            if (nb.a != 9 || !"nueve".equals(nb.b)) return i; i++;                 // 522
            if (!nb.avisoDeInexistente) return i; i++;                             // 523
            // `a` vino del flujo, asi que **no** salio por omision.
            if (nb.porOmision) return i; i++;                                      // 524

            // La validacion corre una vez, y despues de que el grafo esta armado.
            int antes = Validada.validadas;
            vuelta(new Validada());
            if (Validada.validadas != antes + 1) return i; i++;                    // 525

            // `readUnshared`: dos lecturas del mismo objeto dan **dos** instancias.
            Nodo uno = new Nodo("u");
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos);
            oos.writeUnshared(uno);
            oos.writeUnshared(uno);
            oos.flush();
            java.io.ObjectInputStream su = flujo(bos.toByteArray());
            Object u1 = su.readUnshared();
            Object u2 = su.readUnshared();
            if (u1 == u2) return i; i++;                                           // 526
            if (!((Nodo) u1).nombre.equals("u")) return i; i++;                     // 527

            // Una referencia hacia atras **no** se puede leer como no compartida: devolveria
            // justamente el objeto que se pidio no compartir.
            try {
                java.io.ObjectInputStream sc = flujo(bin(ESPERADO[11]));
                sc.readObject();
                sc.readUnshared();
                return i;                                                          // 528
            } catch (java.io.InvalidObjectException esperada) {
                i++;
            }

            // Una cabecera que no es la del formato se rechaza en el constructor, no mas tarde.
            try {
                flujo(bin("cafe000570"));
                return i;                                                          // 529
            } catch (java.io.StreamCorruptedException esperada) {
                i++;
            }

            // Pedir un objeto donde el flujo trae datos primitivos avisa con el largo, que es lo
            // unico que le permite al que llama seguir leyendo.
            try {
                java.io.ByteArrayOutputStream bd = new java.io.ByteArrayOutputStream();
                java.io.ObjectOutputStream od = new java.io.ObjectOutputStream(bd);
                od.writeInt(7);
                od.flush();
                flujo(bd.toByteArray()).readObject();
                return i;                                                          // 530
            } catch (java.io.OptionalDataException esperada) {
                if (esperada.length != 4 || esperada.eof) return i;
                i++;
            }

            // Un filtro que rechaza una clase corta la lectura antes de construirla.
            try {
                java.io.ByteArrayOutputStream bf = new java.io.ByteArrayOutputStream();
                java.io.ObjectOutputStream of = new java.io.ObjectOutputStream(bf);
                of.writeObject(new Punto(1, 1));
                of.flush();
                java.io.ObjectInputStream sf = flujo(bf.toByteArray());
                sf.setObjectInputFilter(new Prohibe(Punto.class));
                sf.readObject();
                return i;                                                          // 531
            } catch (java.io.InvalidClassException esperada) {
                i++;
            }

            // El filtro se fija una sola vez: si se pudiera cambiar no seria una politica.
            try {
                java.io.ObjectInputStream sf2 = flujo(bin(ESPERADO[1]));
                sf2.setObjectInputFilter(new Prohibe(Punto.class));
                sf2.setObjectInputFilter(new Prohibe(Nodo.class));
                return i;                                                          // 532
            } catch (IllegalStateException esperada) {
                i++;
            }

            // `reset()` corta la memoria del flujo: lo que ya se escribio vuelve a salir entero y
            // del otro lado son **dos** objetos.
            Nodo comp = new Nodo("c");
            java.io.ByteArrayOutputStream br = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream or = new java.io.ObjectOutputStream(br);
            or.writeObject(comp);
            or.reset();
            or.writeObject(comp);
            or.flush();
            java.io.ObjectInputStream sr = flujo(br.toByteArray());
            Object r1 = sr.readObject();
            Object r2 = sr.readObject();
            if (r1 == r2) return i; i++;                                           // 533
            if (!((Nodo) r2).nombre.equals("c")) return i; i++;                     // 534

            return -1;
        } catch (Throwable e) {
            return i;
        }
    }


    // ---- getChannel (600) -------------------------------------------------------------------------
    //
    // Lo que `getChannel()` promete no es "un canal sobre el mismo archivo" sino que la posicion del
    // canal y la del flujo son **el mismo numero**. Es la parte que se equivoca sola si cada uno
    // lleva su cuenta, porque nada falla: se lee o se escribe en el lugar que no era y el que llama
    // recibe bytes perfectamente creibles. Por eso cada caso mueve uno y mira el otro.

    private static int canales() {
        int i = 600;
        File f = null;
        File g = null;
        try {
            f = File.createTempFile("kajican", ".bin");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            fos.write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
            fos.close();

            // --- lectura ---
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            java.nio.channels.FileChannel c = in.getChannel();
            // El mismo objeto en cada llamada: el contrato dice "the unique FileChannel object".
            if (c != in.getChannel()) return i; i++;                     // 600
            if (c.position() != 0L) return i; i++;                       // 601
            if (c.size() != 10L) return i; i++;                          // 602
            // Leer del flujo mueve el canal.
            if (in.read() != 0) return i; i++;                           // 603
            if (c.position() != 1L) return i; i++;                       // 604
            // Mover el canal cambia desde donde lee el flujo.
            c.position(5L);
            if (in.read() != 5) return i; i++;                           // 605
            if (c.position() != 6L) return i; i++;                       // 606
            // Leer por el canal tambien mueve el flujo.
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(2);
            if (c.read(bb) != 2) return i; i++;                          // 607
            if (bb.array()[0] != 6 || bb.array()[1] != 7) return i; i++; // 608
            if (in.read() != 8) return i; i++;                           // 609
            // La lectura por posicion absoluta **no** mueve nada.
            java.nio.ByteBuffer abs = java.nio.ByteBuffer.allocate(1);
            if (c.read(abs, 0L) != 1 || abs.array()[0] != 0) return i; i++;  // 610
            if (c.position() != 9L) return i; i++;                        // 611
            // Pasarse del final es legal: `position()` devuelve lo que se puso y se lee -1.
            c.position(100L);
            if (c.position() != 100L) return i; i++;                      // 612
            if (in.read() != -1) return i; i++;                           // 613
            // De solo lectura, como el del JDK.
            try {
                c.write(java.nio.ByteBuffer.allocate(1));
                return i;                                                 // 614
            } catch (java.nio.channels.NonWritableChannelException esperada) { i++; }
            // Cerrar el flujo cierra el canal: son la misma cosa vista de dos maneras.
            in.close();
            if (c.isOpen()) return i; i++;                                // 615

            // --- escritura ---
            g = File.createTempFile("kajican", ".out");
            java.io.FileOutputStream out = new java.io.FileOutputStream(g);
            java.nio.channels.FileChannel w = out.getChannel();
            if (w != out.getChannel()) return i; i++;                     // 616
            out.write(new byte[] { 10, 11, 12 });
            // La posicion cuenta lo escrito por el flujo, **buffer incluido**: es justo el numero
            // que se desincronizaria si el canal no vaciara lo pendiente antes de contestar.
            if (w.position() != 3L) return i; i++;                        // 617
            if (w.size() != 3L) return i; i++;                            // 618
            // Escribir por el canal continua donde iba el flujo.
            w.write(java.nio.ByteBuffer.wrap(new byte[] { 13 }));
            if (w.position() != 4L) return i; i++;                        // 619
            // Y el flujo sigue despues del canal.
            out.write(14);
            out.close();
            byte[] leido = new byte[8];
            java.io.FileInputStream v = new java.io.FileInputStream(g);
            int n = v.read(leido);
            v.close();
            if (n != 5) return i; i++;                                    // 620
            if (leido[0] != 10 || leido[3] != 13 || leido[4] != 14) return i; i++;  // 621

            // Mover el canal hacia atras cambia **donde escribe el flujo**, que es la mitad del
            // contrato que se pierde si el flujo sigue agregando al final por su cuenta.
            java.io.FileOutputStream o2 = new java.io.FileOutputStream(g);
            java.nio.channels.FileChannel w2 = o2.getChannel();
            o2.write(new byte[] { 1, 2, 3, 4 });
            w2.position(1L);
            o2.write(new byte[] { 9 });
            o2.close();
            byte[] l2 = new byte[8];
            java.io.FileInputStream v2 = new java.io.FileInputStream(g);
            int n2 = v2.read(l2);
            v2.close();
            if (n2 != 4) return i; i++;                                   // 622
            if (l2[0] != 1 || l2[1] != 9 || l2[2] != 3 || l2[3] != 4) return i; i++;  // 623

            return -1;
        } catch (Throwable e) {
            return i;
        } finally {
            if (f != null) f.delete();
            if (g != null) g.delete();
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("ser")) {
            try {
                String[] h = casos();
                for (int k = 0; k < h.length; k++) System.out.println(h[k]);
            } catch (IOException e) {
                System.out.println("ERR " + e);
            }
            return;
        }
        if (args.length > 0 && args[0].equals("uids")) {
            System.out.println(ObjectStreamClass.lookup(Simple.class).getSerialVersionUID());
            System.out.println(ObjectStreamClass.lookup(ConSuid.class).getSerialVersionUID());
            System.out.println(ObjectStreamClass.lookup(ConClinit.class).getSerialVersionUID());
            System.out.println(ObjectStreamClass.lookup(Marca.class).getSerialVersionUID());
            System.out.println(ObjectStreamClass.lookup(ConPrivados.class).getSerialVersionUID());
            return;
        }
        System.out.println(run());
    }
}
