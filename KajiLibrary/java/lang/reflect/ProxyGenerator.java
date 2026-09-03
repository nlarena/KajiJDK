package java.lang.reflect;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * KajiLibrary's java.lang.reflect.ProxyGenerator -- el escritor de archivos de clase que hay
 * detras de {@link Proxy}. Package-private, igual que en el JDK: nadie fuera de `Proxy` tiene por
 * que saber que un proxy es, literalmente, un `.class` fabricado en memoria.
 *
 * <h2>Por que este generador y no `java.lang.classfile`</h2>
 *
 * <p>`java.lang.classfile` esta en KajiLibrary como fuente, pero solo del lado de la LECTURA:
 * escribir codigo con el exige el paquete `java.lang.classfile.instruction` entero y un generador
 * de `StackMapTable`, y ninguna de las dos cosas existe. Un proxy no necesita nada de eso, asi
 * que este archivo emite el subconjunto exacto que un proxy usa y nada mas.
 *
 * <h2>Por que la version 49 del archivo de clase</h2>
 *
 * <p>La `StackMapTable` es obligatoria a partir de la version 50 (Java 6) y es, con diferencia,
 * la parte cara de generar bytecode: hay que calcular el estado de tipos en cada destino de
 * salto. La version 49 (Java 5) usa el verificador viejo, el de INFERENCIA, que deduce esos tipos
 * solo -- y el verificador de KajiJDK implementa las dos formas (ver `src/jvm/verifier.rs`), asi
 * que emitir 49 es legal aca y ahorra el generador de mapas de pila entero.
 *
 * <p>El costo de esa eleccion es cero en la practica: los metodos que este generador emite no
 * tienen NI UN salto. Son rectos de punta a punta -- armar el arreglo, llamar al despachador,
 * castear el retorno --, y un metodo sin saltos no necesita mapa de pila ni siquiera en la
 * version 69. La version 49 es lo que hace que eso sea ademas *legal*, no solo suficiente.
 *
 * <h2>Que emite cada metodo</h2>
 *
 * <p>La segunda decision que ahorra la mitad del trabajo: el bytecode NO busca el {@link Method}
 * ni lee el campo `h`. Cada metodo generado empuja `this`, un `int` con su indice en la tabla de
 * metodos de esa clase, y un `Object[]` con los argumentos ya boxeados; despues llama a un solo
 * metodo estatico, `jdk.internal.reflect.ProxyDispatcher.despachar`, que hace todo el resto EN
 * JAVA: busca el `Method`, saca el manejador, lo llama y traduce las excepciones. Lo que queda en
 * bytecode es lo unico que no se puede escribir en Java -- una firma que no existe en el fuente.
 *
 * <p>El castear/desboxear del retorno tampoco es adorno: es lo que le da al proxy la semantica
 * que el JDK documenta, y gratis. `checkcast Integer` sobre un `null` pasa, y el `intValue()` que
 * viene despues tira `NullPointerException` -- que es exactamente lo que el JDK promete para un
 * `null` devuelto por un metodo de retorno primitivo. Un valor del tipo equivocado muere en el
 * `checkcast` con `ClassCastException`, que es la otra mitad de la promesa. Ninguna de las dos
 * esta programada; las dos caen solas de elegir estas dos instrucciones.
 */
final class ProxyGenerator {

    /** Ver la nota de la clase: 49 es la ultima version sin `StackMapTable` obligatoria. */
    private static final int VERSION_MAYOR = 49;
    private static final int VERSION_MENOR = 0;

    static final int ACC_PUBLIC = 0x0001;
    static final int ACC_FINAL = 0x0010;
    static final int ACC_SUPER = 0x0020;

    // Etiquetas del pool de constantes (JVMS 4.4). Solo estas cinco: sin invokedynamic, sin
    // literales de String y sin constantes de 8 bytes, el pool de un proxy es minusculo.
    private static final int TAG_UTF8 = 1;
    private static final int TAG_INTEGER = 3;
    private static final int TAG_CLASS = 7;
    private static final int TAG_METHODREF = 10;
    private static final int TAG_NAMEANDTYPE = 12;

    // Los opcodes que se usan. Estan como constantes con nombre porque un `0x2a` suelto en el
    // medio de un emisor es indistinguible de un error de tipeo.
    private static final int OP_ACONST_NULL = 0x01;
    private static final int OP_ICONST_0 = 0x03;
    private static final int OP_BIPUSH = 0x10;
    private static final int OP_SIPUSH = 0x11;
    private static final int OP_LDC_W = 0x13;
    private static final int OP_ILOAD = 0x15;
    private static final int OP_LLOAD = 0x16;
    private static final int OP_FLOAD = 0x17;
    private static final int OP_DLOAD = 0x18;
    private static final int OP_ALOAD = 0x19;
    private static final int OP_ILOAD_0 = 0x1a;
    private static final int OP_LLOAD_0 = 0x1e;
    private static final int OP_FLOAD_0 = 0x22;
    private static final int OP_DLOAD_0 = 0x26;
    private static final int OP_ALOAD_0 = 0x2a;
    private static final int OP_AASTORE = 0x53;
    private static final int OP_POP = 0x57;
    private static final int OP_DUP = 0x59;
    private static final int OP_IRETURN = 0xac;
    private static final int OP_LRETURN = 0xad;
    private static final int OP_FRETURN = 0xae;
    private static final int OP_DRETURN = 0xaf;
    private static final int OP_ARETURN = 0xb0;
    private static final int OP_RETURN = 0xb1;
    private static final int OP_INVOKEVIRTUAL = 0xb6;
    private static final int OP_INVOKESPECIAL = 0xb7;
    private static final int OP_INVOKESTATIC = 0xb8;
    private static final int OP_ANEWARRAY = 0xbd;
    private static final int OP_CHECKCAST = 0xc0;

    private static final String PROXY = "java/lang/reflect/Proxy";
    private static final String MANEJADOR = "Ljava/lang/reflect/InvocationHandler;";
    private static final String DESPACHADOR = "jdk/internal/reflect/ProxyDispatcher";
    private static final String FIRMA_DESPACHO =
            "(Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;";

    /** Las entradas del pool, ya serializadas, en orden; el indice de una es su posicion + 1. */
    private final ArrayList<byte[]> pool = new ArrayList<byte[]>();

    /** De clave (ver {@link #agregar}) a indice, para que una constante repetida entre una vez. */
    private final HashMap<String, Integer> indices = new HashMap<String, Integer>();

    private ProxyGenerator() {
    }

    // ------------------------------------------------------------------ buffer

    /**
     * Un arreglo de bytes que crece. No se usa `ByteArrayOutputStream` porque este generador vive
     * en `java.lang.reflect` y arrastrar `java.io` hasta aca para escribir cuatro enteros grandes
     * no compra nada.
     */
    private static final class Buf {
        private byte[] datos = new byte[128];
        private int largo;

        void u1(int v) {
            this.espacio(1);
            this.datos[this.largo] = (byte) v;
            this.largo = this.largo + 1;
        }

        void u2(int v) {
            this.u1(v >> 8);
            this.u1(v);
        }

        void u4(int v) {
            this.u2(v >> 16);
            this.u2(v);
        }

        void crudo(byte[] otros) {
            this.espacio(otros.length);
            System.arraycopy(otros, 0, this.datos, this.largo, otros.length);
            this.largo = this.largo + otros.length;
        }

        private void espacio(int n) {
            if (this.largo + n <= this.datos.length) {
                return;
            }
            int nuevo = this.datos.length * 2;
            if (nuevo < this.largo + n) {
                nuevo = this.largo + n;
            }
            byte[] mas = new byte[nuevo];
            System.arraycopy(this.datos, 0, mas, 0, this.largo);
            this.datos = mas;
        }

        byte[] listo() {
            byte[] salida = new byte[this.largo];
            System.arraycopy(this.datos, 0, salida, 0, this.largo);
            return salida;
        }

        int tamanio() {
            return this.largo;
        }
    }

    /**
     * El cuerpo de un metodo, con la altura de la pila contada mientras se emite.
     *
     * <p>Se cuenta en vez de estimarse porque `max_stack` no es un adorno: el interprete
     * dimensiona el marco con el, y un valor bajo es corrupcion silenciosa mientras que uno alto
     * es desperdicio. Como no hay saltos, la altura en cada punto es una sola -- contarla es
     * sumar un delta por instruccion, y da el maximo exacto.
     */
    private static final class Codigo {
        final Buf b = new Buf();
        private int pila;
        private int pilaMax;

        void mover(int delta) {
            this.pila = this.pila + delta;
            if (this.pila > this.pilaMax) {
                this.pilaMax = this.pila;
            }
        }

        void op(int opcode, int delta) {
            this.b.u1(opcode);
            this.mover(delta);
        }

        int pilaMax() {
            return this.pilaMax;
        }
    }

    // ------------------------------------------------------------ pool de constantes

    /**
     * Devuelve el indice de una constante, agregandola si no estaba.
     *
     * <p>`clave` lleva un prefijo por etiqueta porque los espacios de nombres se cruzan: el Utf8
     * "java/lang/Object" y la Class "java/lang/Object" son entradas distintas con el mismo texto.
     */
    private int agregar(String clave, byte[] entrada) {
        Integer ya = this.indices.get(clave);
        if (ya != null) {
            return ya.intValue();
        }
        this.pool.add(entrada);
        int indice = this.pool.size();
        this.indices.put(clave, Integer.valueOf(indice));
        return indice;
    }

    private int utf(String texto) {
        byte[] codificado = ProxyGenerator.utf8Modificado(texto);
        Buf b = new Buf();
        b.u1(TAG_UTF8);
        b.u2(codificado.length);
        b.crudo(codificado);
        return this.agregar("u:" + texto, b.listo());
    }

    private int clase(String interno) {
        int nombre = this.utf(interno);
        Buf b = new Buf();
        b.u1(TAG_CLASS);
        b.u2(nombre);
        return this.agregar("c:" + interno, b.listo());
    }

    private int nombreYTipo(String nombre, String descriptor) {
        int n = this.utf(nombre);
        int d = this.utf(descriptor);
        Buf b = new Buf();
        b.u1(TAG_NAMEANDTYPE);
        b.u2(n);
        b.u2(d);
        return this.agregar("n:" + nombre + " " + descriptor, b.listo());
    }

    private int metodoRef(String duenio, String nombre, String descriptor) {
        int c = this.clase(duenio);
        int nt = this.nombreYTipo(nombre, descriptor);
        Buf b = new Buf();
        b.u1(TAG_METHODREF);
        b.u2(c);
        b.u2(nt);
        return this.agregar("m:" + duenio + " " + nombre + " " + descriptor, b.listo());
    }

    private int enteroConstante(int valor) {
        Buf b = new Buf();
        b.u1(TAG_INTEGER);
        b.u4(valor);
        return this.agregar("I:" + valor, b.listo());
    }

    /**
     * UTF-8 modificado (JVMS 4.4.7): igual al UTF-8 real salvo que el cero se codifica en dos
     * bytes y los pares suplentes se codifican por separado. Se implementa a mano y no con
     * `String.getBytes` porque `getBytes` produce UTF-8 real, que para esos dos casos es otro
     * arreglo de bytes y un archivo de clase que el parser rechaza.
     */
    private static byte[] utf8Modificado(String texto) {
        Buf b = new Buf();
        int i = 0;
        while (i < texto.length()) {
            char c = texto.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                b.u1(c);
            } else if (c <= 0x07FF) {
                b.u1(0xC0 | (c >> 6));
                b.u1(0x80 | (c & 0x3F));
            } else {
                b.u1(0xE0 | (c >> 12));
                b.u1(0x80 | ((c >> 6) & 0x3F));
                b.u1(0x80 | (c & 0x3F));
            }
            i = i + 1;
        }
        return b.listo();
    }

    // ------------------------------------------------------------------ descriptores

    /** El nombre interno de un tipo: `java.util.List` -> `java/util/List`. */
    static String interno(Class<?> tipo) {
        return tipo.getName().replace('.', '/');
    }

    /** El descriptor de campo de un tipo (JVMS 4.3.2). */
    static String descriptor(Class<?> tipo) {
        if (tipo.isPrimitive()) {
            return ProxyGenerator.descriptorPrimitivo(tipo);
        }
        if (tipo.isArray()) {
            return "[" + ProxyGenerator.descriptor(tipo.getComponentType());
        }
        return "L" + ProxyGenerator.interno(tipo) + ";";
    }

    private static String descriptorPrimitivo(Class<?> tipo) {
        if (tipo == Void.TYPE) {
            return "V";
        }
        if (tipo == Boolean.TYPE) {
            return "Z";
        }
        if (tipo == Byte.TYPE) {
            return "B";
        }
        if (tipo == Character.TYPE) {
            return "C";
        }
        if (tipo == Short.TYPE) {
            return "S";
        }
        if (tipo == Integer.TYPE) {
            return "I";
        }
        if (tipo == Long.TYPE) {
            return "J";
        }
        if (tipo == Float.TYPE) {
            return "F";
        }
        return "D";
    }

    /** El descriptor de metodo de una firma: `(params)retorno`. */
    static String descriptorDeMetodo(Class<?>[] parametros, Class<?> retorno) {
        StringBuilder out = new StringBuilder("(");
        int i = 0;
        while (i < parametros.length) {
            out.append(ProxyGenerator.descriptor(parametros[i]));
            i = i + 1;
        }
        out.append(")");
        out.append(ProxyGenerator.descriptor(retorno));
        return out.toString();
    }

    /** Cuantas ranuras ocupa un tipo en el marco: dos para `long` y `double`, una para el resto. */
    private static int ranuras(Class<?> tipo) {
        if (tipo == Long.TYPE || tipo == Double.TYPE) {
            return 2;
        }
        return 1;
    }

    /** La clase envoltorio de un primitivo, en forma interna; `null` si `tipo` no es primitivo. */
    private static String envoltorio(Class<?> tipo) {
        if (tipo == Boolean.TYPE) {
            return "java/lang/Boolean";
        }
        if (tipo == Byte.TYPE) {
            return "java/lang/Byte";
        }
        if (tipo == Character.TYPE) {
            return "java/lang/Character";
        }
        if (tipo == Short.TYPE) {
            return "java/lang/Short";
        }
        if (tipo == Integer.TYPE) {
            return "java/lang/Integer";
        }
        if (tipo == Long.TYPE) {
            return "java/lang/Long";
        }
        if (tipo == Float.TYPE) {
            return "java/lang/Float";
        }
        if (tipo == Double.TYPE) {
            return "java/lang/Double";
        }
        return null;
    }

    /** El nombre del metodo que saca el primitivo de su envoltorio: `intValue`, `charValue`... */
    private static String desboxeador(Class<?> tipo) {
        if (tipo == Boolean.TYPE) {
            return "booleanValue";
        }
        if (tipo == Byte.TYPE) {
            return "byteValue";
        }
        if (tipo == Character.TYPE) {
            return "charValue";
        }
        if (tipo == Short.TYPE) {
            return "shortValue";
        }
        if (tipo == Integer.TYPE) {
            return "intValue";
        }
        if (tipo == Long.TYPE) {
            return "longValue";
        }
        if (tipo == Float.TYPE) {
            return "floatValue";
        }
        return "doubleValue";
    }

    // ------------------------------------------------------------------ emision

    /**
     * Fabrica el archivo de clase de un proxy.
     *
     * @param nombreBinario el nombre con puntos de la clase a generar
     * @param interfaces las interfaces que implementa, en orden
     * @param publica si la clase lleva `ACC_PUBLIC`
     * @param metodos la tabla de metodos, en el mismo orden en que el despachador la va a indexar
     * @return los bytes del `.class`
     */
    static byte[] generar(String nombreBinario, Class<?>[] interfaces, boolean publica,
            Method[] metodos) {
        ProxyGenerator gen = new ProxyGenerator();
        return gen.armar(nombreBinario, interfaces, publica, metodos);
    }

    private byte[] armar(String nombreBinario, Class<?>[] interfaces, boolean publica,
            Method[] metodos) {
        String estaClase = nombreBinario.replace('.', '/');

        // Los metodos se serializan PRIMERO: cada uno mete constantes en el pool, y el pool solo
        // se puede escribir cuando ya nadie le va a agregar nada.
        ArrayList<byte[]> cuerpos = new ArrayList<byte[]>();
        cuerpos.add(this.constructor());
        int i = 0;
        while (i < metodos.length) {
            cuerpos.add(this.metodoProxy(estaClase, metodos[i], i));
            i = i + 1;
        }

        int indiceEsta = this.clase(estaClase);
        int indiceSuper = this.clase(PROXY);
        int[] indicesInterfaces = new int[interfaces.length];
        i = 0;
        while (i < interfaces.length) {
            indicesInterfaces[i] = this.clase(ProxyGenerator.interno(interfaces[i]));
            i = i + 1;
        }

        Buf out = new Buf();
        out.u4(0xCAFEBABE);
        out.u2(VERSION_MENOR);
        out.u2(VERSION_MAYOR);
        // constant_pool_count es "la cantidad + 1": la entrada 0 no existe y se cuenta igual.
        out.u2(this.pool.size() + 1);
        i = 0;
        while (i < this.pool.size()) {
            out.crudo(this.pool.get(i));
            i = i + 1;
        }
        int acceso = ACC_FINAL | ACC_SUPER;
        if (publica) {
            acceso = acceso | ACC_PUBLIC;
        }
        out.u2(acceso);
        out.u2(indiceEsta);
        out.u2(indiceSuper);
        out.u2(indicesInterfaces.length);
        i = 0;
        while (i < indicesInterfaces.length) {
            out.u2(indicesInterfaces[i]);
            i = i + 1;
        }
        out.u2(0); // fields_count: el unico campo que un proxy tiene es `h`, y lo hereda
        out.u2(cuerpos.size());
        i = 0;
        while (i < cuerpos.size()) {
            out.crudo(cuerpos.get(i));
            i = i + 1;
        }
        out.u2(0); // attributes_count
        return out.listo();
    }

    /**
     * `public $Proxy0(InvocationHandler h) { super(h); }`.
     *
     * <p>Es publico y no protegido a proposito: `Proxy.newProxyInstance` lo busca por reflexion y
     * lo invoca desde otro paquete, que es exactamente lo que hace el JDK.
     */
    private byte[] constructor() {
        Codigo c = new Codigo();
        c.op(OP_ALOAD_0, 1);
        c.op(OP_ALOAD_0 + 1, 1); // aload_1: el manejador
        c.b.u1(OP_INVOKESPECIAL);
        c.b.u2(this.metodoRef(PROXY, "<init>", "(" + MANEJADOR + ")V"));
        c.mover(-2);
        c.op(OP_RETURN, 0);
        return this.metodoInfo(ACC_PUBLIC, "<init>", "(" + MANEJADOR + ")V",
                c.b.listo(), c.pilaMax(), 2, null);
    }

    /**
     * Un metodo del proxy: empaquetar, despachar, castear.
     *
     * <p>`indice` es la posicion del metodo en la tabla que `ProxyDispatcher` tiene registrada
     * para esta clase. Numerar en vez de nombrar es lo que deja el bytecode sin ninguna busqueda:
     * el despachador indexa un arreglo y ya tiene el {@link Method}.
     */
    private byte[] metodoProxy(String estaClase, Method metodo, int indice) {
        Class<?>[] parametros = metodo.getParameterTypes();
        Class<?> retorno = metodo.getReturnType();
        Codigo c = new Codigo();

        c.op(OP_ALOAD_0, 1); // el proxy, primer argumento del despachador
        this.empujarEntero(c, indice);

        if (parametros.length == 0) {
            // `null` y no un arreglo vacio: es lo que el JDK le pasa al manejador cuando el
            // metodo no toma nada, y hay codigo escrito contra eso.
            c.op(OP_ACONST_NULL, 1);
        } else {
            this.empujarEntero(c, parametros.length);
            c.b.u1(OP_ANEWARRAY);
            c.b.u2(this.clase("java/lang/Object"));
            c.mover(0); // consume el largo, deja la referencia
            int ranura = 1; // 0 es `this`
            int i = 0;
            while (i < parametros.length) {
                c.op(OP_DUP, 1);
                this.empujarEntero(c, i);
                this.cargar(c, parametros[i], ranura);
                this.boxear(c, parametros[i]);
                c.op(OP_AASTORE, -3);
                ranura = ranura + ProxyGenerator.ranuras(parametros[i]);
                i = i + 1;
            }
        }

        c.b.u1(OP_INVOKESTATIC);
        c.b.u2(this.metodoRef(DESPACHADOR, "despachar", FIRMA_DESPACHO));
        c.mover(-2); // entran tres, sale uno

        this.retornar(c, retorno);

        int localesMax = 1;
        int i2 = 0;
        while (i2 < parametros.length) {
            localesMax = localesMax + ProxyGenerator.ranuras(parametros[i2]);
            i2 = i2 + 1;
        }
        String descriptor = ProxyGenerator.descriptorDeMetodo(parametros, retorno);
        return this.metodoInfo(ACC_PUBLIC | ACC_FINAL, metodo.getName(), descriptor,
                c.b.listo(), c.pilaMax(), localesMax, metodo.getExceptionTypes());
    }

    /** El literal entero mas corto que sirva: `iconst_N`, `bipush`, `sipush` o `ldc_w`. */
    private void empujarEntero(Codigo c, int valor) {
        if (valor >= 0 && valor <= 5) {
            c.op(OP_ICONST_0 + valor, 1);
        } else if (valor >= -128 && valor <= 127) {
            c.b.u1(OP_BIPUSH);
            c.b.u1(valor);
            c.mover(1);
        } else if (valor >= -32768 && valor <= 32767) {
            c.b.u1(OP_SIPUSH);
            c.b.u2(valor);
            c.mover(1);
        } else {
            c.b.u1(OP_LDC_W);
            c.b.u2(this.enteroConstante(valor));
            c.mover(1);
        }
    }

    /** Carga el parametro de la ranura `ranura` con la instruccion que su tipo pide. */
    private void cargar(Codigo c, Class<?> tipo, int ranura) {
        int base;
        int ancho;
        if (tipo == Long.TYPE) {
            base = OP_LLOAD_0;
            ancho = OP_LLOAD;
        } else if (tipo == Float.TYPE) {
            base = OP_FLOAD_0;
            ancho = OP_FLOAD;
        } else if (tipo == Double.TYPE) {
            base = OP_DLOAD_0;
            ancho = OP_DLOAD;
        } else if (tipo.isPrimitive()) {
            base = OP_ILOAD_0;
            ancho = OP_ILOAD;
        } else {
            base = OP_ALOAD_0;
            ancho = OP_ALOAD;
        }
        int delta = ProxyGenerator.ranuras(tipo);
        if (ranura <= 3) {
            c.op(base + ranura, delta);
        } else {
            // Sin forma `wide`: mas de 255 ranuras de parametros no lo emite ni javac, porque el
            // descriptor de metodo tampoco lo admite (JVMS 4.3.3).
            c.b.u1(ancho);
            c.b.u1(ranura);
            c.mover(delta);
        }
    }

    /** Envuelve el primitivo que quedo arriba de la pila; un no-primitivo ya esta listo. */
    private void boxear(Codigo c, Class<?> tipo) {
        String caja = ProxyGenerator.envoltorio(tipo);
        if (caja == null) {
            return;
        }
        String firma = "(" + ProxyGenerator.descriptor(tipo) + ")L" + caja + ";";
        c.b.u1(OP_INVOKESTATIC);
        c.b.u2(this.metodoRef(caja, "valueOf", firma));
        c.mover(1 - ProxyGenerator.ranuras(tipo));
    }

    /**
     * Cierra el metodo con el retorno que su tipo pide.
     *
     * <p>Aca es donde caen solas dos clausulas del contrato: sobre `null`, el `checkcast` pasa y
     * el desboxeo tira `NullPointerException`; sobre un valor de otro tipo, el `checkcast` tira
     * `ClassCastException`. Ninguna de las dos esta escrita en ningun lado.
     */
    private void retornar(Codigo c, Class<?> retorno) {
        if (retorno == Void.TYPE) {
            // Lo que el manejador haya devuelto se descarta, incluso si no es `null`.
            c.op(OP_POP, -1);
            c.op(OP_RETURN, 0);
            return;
        }
        String caja = ProxyGenerator.envoltorio(retorno);
        if (caja == null) {
            c.b.u1(OP_CHECKCAST);
            c.b.u2(this.clase(ProxyGenerator.interno(retorno)));
            c.mover(0);
            c.op(OP_ARETURN, -1);
            return;
        }
        c.b.u1(OP_CHECKCAST);
        c.b.u2(this.clase(caja));
        c.mover(0);
        String nombre = ProxyGenerator.desboxeador(retorno);
        c.b.u1(OP_INVOKEVIRTUAL);
        c.b.u2(this.metodoRef(caja, nombre, "()" + ProxyGenerator.descriptor(retorno)));
        c.mover(ProxyGenerator.ranuras(retorno) - 1);
        if (retorno == Long.TYPE) {
            c.op(OP_LRETURN, -2);
        } else if (retorno == Float.TYPE) {
            c.op(OP_FRETURN, -1);
        } else if (retorno == Double.TYPE) {
            c.op(OP_DRETURN, -2);
        } else {
            c.op(OP_IRETURN, -1);
        }
    }

    /**
     * Un `method_info` entero: el encabezado, el `Code` y -- si el metodo declara alguna -- el
     * atributo `Exceptions`.
     *
     * <p>El `Exceptions` no lo lee ni el verificador ni el interprete: es para que
     * `getDeclaredMethods()` sobre la clase generada diga la verdad sobre lo que sus metodos
     * declaran. Un proxy que miente sobre eso es un proxy que no reemplaza a la interfaz.
     */
    private byte[] metodoInfo(int acceso, String nombre, String descriptor, byte[] codigo,
            int pilaMax, int localesMax, Class<?>[] excepciones) {
        int cantidadExcepciones = excepciones == null ? 0 : excepciones.length;
        Buf b = new Buf();
        b.u2(acceso);
        b.u2(this.utf(nombre));
        b.u2(this.utf(descriptor));
        b.u2(cantidadExcepciones > 0 ? 2 : 1);

        b.u2(this.utf("Code"));
        // max_stack + max_locals + code_length + el codigo + exception_table_length +
        // attributes_count = 2 + 2 + 4 + n + 2 + 2.
        b.u4(12 + codigo.length);
        b.u2(pilaMax);
        b.u2(localesMax);
        b.u4(codigo.length);
        b.crudo(codigo);
        b.u2(0); // sin manejadores: la traduccion de excepciones la hace el despachador, en Java
        b.u2(0); // sin LineNumberTable ni StackMapTable

        if (cantidadExcepciones > 0) {
            b.u2(this.utf("Exceptions"));
            b.u4(2 + 2 * cantidadExcepciones);
            b.u2(cantidadExcepciones);
            int i = 0;
            while (i < cantidadExcepciones) {
                b.u2(this.clase(ProxyGenerator.interno(excepciones[i])));
                i = i + 1;
            }
        }
        return b.listo();
    }
}
