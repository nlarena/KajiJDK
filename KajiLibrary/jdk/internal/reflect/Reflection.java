package jdk.internal.reflect;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * KajiLibrary's jdk.internal.reflect.Reflection — las reglas de acceso de la reflexion, y el filtro
 * de miembros que la reflexion no debe entregar.
 *
 * <h2>Dos mitades independientes</h2>
 *
 * <p>La <strong>primera</strong> es el chequeo de acceso: {@link #verifyMemberAccess} contesta la
 * pregunta que la JLS §6.6 le hace a cada acceso —¿puede esta clase tocar este miembro?— y es
 * aritmetica pura sobre modificadores, paquetes, nidos y la jerarquia de herencia. No necesita nada
 * del VM que esta biblioteca no tenga ya, asi que esta entera y contesta lo mismo que el JDK.
 *
 * <p>La <strong>segunda</strong> es el registro de filtrado: una clase puede declarar que ciertos
 * campos o metodos suyos <em>no existen</em> para la reflexion, y {@link #filterFields} /
 * {@link #filterMethods} los sacan del arreglo. El mapa arranca con las mismas entradas que el del
 * JDK, que son las que impiden que {@code ClassLoader.getDeclaredFields()} entregue las tripas del
 * cargador de clases.
 *
 * <p>Vale decir en que difiere aca: en el JDK ese filtro esta <em>enchufado</em>, porque
 * {@code Class.getDeclaredFields0} pasa por el. En esta VM {@code Class.getDeclaredFields} es un
 * nativo que no consulta a nadie, asi que el filtro es una funcion que hay que llamar y no una que se
 * aplique sola. La funcion hace lo que promete; lo que no hay es el gancho que la llamaria, y eso es
 * de {@code java.lang.Class}, no de esta clase.
 *
 * <h2>Los cinco miembros que no estan</h2>
 *
 * <ul>
 * <li><strong>{@code getCallerClass()}</strong> — es el gancho: en HotSpot el VM recorre la pila
 *     salteando los frames de la maquinaria reflexiva y de los metodos marcados
 *     {@code @CallerSensitive}. Esta VM expone la pila por {@code jdk.internal.vm.Stack.frames()},
 *     pero como texto ({@code String[]}) y no como {@code Class[]}: reconstruir la clase de un nombre
 *     impreso pasa por {@code Class.forName}, que carga y <em>inicializa</em>, y el resultado seria
 *     otra clase que la del frame en cuanto haya dos cargadores. Devolver eso seria peor que no
 *     devolver nada, porque el que llama lo usaria para decidir un acceso.</li>
 * <li><strong>{@code getClassAccessFlags(Class)}</strong> — su razon de ser es diferir de
 *     {@link Class#getModifiers()}: devuelve los bits crudos del archivo de clase, sin la correccion
 *     que {@code getModifiers} hace leyendo el atributo {@code InnerClasses}. Aca
 *     {@code getModifiers} ya devuelve los crudos —no hay tal correccion— salvo por un bit:
 *     {@code ACC_SUPER}, que el nativo saca a proposito para que {@code Modifier.toString} no imprima
 *     clases "synchronized". O sea que el valor honesto y el que hay difieren justo en el bit que
 *     este metodo existe para no perder. Un miembro que devuelve el numero casi correcto es la clase
 *     de miembro que no se escribe.</li>
 * <li><strong>{@code isCallerSensitive(Method)}</strong> — es leerle una anotacion a un
 *     {@link Method}, y la reflexion de anotaciones a nivel metodo no esta cableada en esta
 *     biblioteca ({@code Method} ni siquiera define {@code getDeclaredAnnotations}). Contestar
 *     {@code false} siempre seria correcto por accidente hoy y falso el dia que se cablee.</li>
 * <li><strong>{@code isTrustedFinalField(Field)}</strong> — pregunta por el bit {@code trustedFinal}
 *     que el VM le escribe al {@code Field} al fabricarlo, y que no es derivable de la superficie
 *     publica: "final de verdad" no es "final". El VM de aca no lo escribe.</li>
 * <li><strong>{@code ensureNativeAccess(...)}</strong> — es un control, y su respuesta correcta
 *     depende de un permiso por modulo ({@code --enable-native-access}) que este runtime no modela.
 *     Un control que siempre deja pasar no es un control laxo, es un control que miente.</li>
 * </ul>
 *
 * <h2>Y las tres clases del paquete que tampoco estan</h2>
 *
 * <p>Este es el lugar donde anotarlas porque las tres cuelgan de lo de arriba.
 *
 * <ul>
 * <li><strong>{@code @CallerSensitive}</strong> y <strong>{@code @CallerSensitiveAdapter}</strong>.
 *     Son declaraciones puras, y por eso la tentacion: una anotacion no tiene cuerpo que pueda
 *     mentir. Pero una marca no significa nada sin quien la lea, y en el JDK la lee <em>el VM</em>:
 *     es lo que hace que {@code getCallerClass} saltee el frame del metodo marcado. Sin ese lector
 *     —y aca no esta, por lo que dice el primer item de la lista de arriba— traerlas seria darle a
 *     alguien la manera de escribir {@code @CallerSensitive} sobre un metodo suyo y creer que dijo
 *     algo. No es el caso de {@code VMSupport.AnnotationDecoder}, que si entro siendo una
 *     declaracion sin usuarios: el contrato de esa interfaz es entre quien la llama y quien la
 *     implementa, y se cumple entero adentro de Java.</li>
 * <li><strong>{@code AccessorUtils}</strong>. Su superficie publica es, entera, un constructor sin
 *     argumentos: el unico metodo que tiene —{@code isIllegalArgument}— es package-private, o sea que
 *     no es API ni en el JDK. Y lo que hace es decidir si un {@code ClassCastException} salido de un
 *     {@code MethodHandle} nacio del accesor o del metodo destino, que es una pregunta que solo
 *     existe si los accesores estan hechos de {@code MethodHandle} — y los de aca no lo estan.
 *     Traerla sumaria una clase al conteo y cero comportamiento, que es exactamente lo que no se
 *     hace.</li>
 * </ul>
 */
public class Reflection {

    // El comodin. El JDK lo compara por contenido y no por identidad, asi que cualquier conjunto que
    // lo contenga filtra todo -- `ALL_MEMBERS` es la manera comoda de escribirlo, no la unica.
    private static final String COMODIN = "*";

    /** El conjunto que, registrado para una clase, esconde <em>todos</em> sus miembros. */
    public static final Set<String> ALL_MEMBERS = Set.of(Reflection.COMODIN);

    // Copia-al-escribir: `filterFields` lee sin candado y `registerFieldsToFilter` publica un mapa
    // nuevo entero. Es lo que permite que el registro sea `synchronized` y la lectura no, que importa
    // porque se filtra en cada `getDeclaredFields` y se registra un puñado de veces en la vida del
    // proceso.
    private static volatile Map<Class<?>, Set<String>> filtroDeCampos;
    private static volatile Map<Class<?>, Set<String>> filtroDeMetodos;

    static {
        // Las mismas entradas que el JDK. Son las clases cuyos campos internos, entregados por
        // reflexion, dejarian escribir el cargador de clases o el bit de accesibilidad de un
        // `AccessibleObject` -- o sea, saltarse todo lo demas que hay en este archivo.
        Map<Class<?>, Set<String>> campos = new HashMap<Class<?>, Set<String>>();
        campos.put(Reflection.class, Reflection.ALL_MEMBERS);
        campos.put(AccessibleObject.class, Reflection.ALL_MEMBERS);
        campos.put(Class.class, Set.of("classLoader", "classData", "modifiers", "protectionDomain",
                "primitive"));
        campos.put(ClassLoader.class, Reflection.ALL_MEMBERS);
        campos.put(Constructor.class, Reflection.ALL_MEMBERS);
        campos.put(Field.class, Reflection.ALL_MEMBERS);
        campos.put(Method.class, Reflection.ALL_MEMBERS);
        campos.put(Module.class, Reflection.ALL_MEMBERS);
        Reflection.filtroDeCampos = campos;
        Reflection.filtroDeMetodos = new HashMap<Class<?>, Set<String>>();
    }

    public Reflection() {
    }

    // ---- el chequeo de acceso (JLS 6.6) ----

    /**
     * Whether {@code currentClass} may touch a member of {@code memberClass} with these modifiers.
     *
     * @param currentClass quien accede
     * @param memberClass quien declara el miembro
     * @param targetClass el tipo estatico del receptor, o {@code null} si el miembro es estatico.
     *                    Solo lo mira la regla de los {@code protected} de instancia (JLS 6.6.2).
     * @param modifiers los modificadores del miembro
     * @return si el acceso es legal
     */
    public static boolean verifyMemberAccess(Class<?> currentClass, Class<?> memberClass,
                                             Class<?> targetClass, int modifiers) {
        Objects.requireNonNull(currentClass);
        Objects.requireNonNull(memberClass);

        // Una clase siempre puede consigo misma, incluido lo privado.
        if (currentClass == memberClass) {
            return true;
        }
        if (!Reflection.verifyModuleAccess(currentClass.getModule(), memberClass)) {
            return false;
        }

        // Dos preguntas de paquete distintas y la misma respuesta: se calcula a lo sumo una vez
        // porque comparar paquetes toca el cargador de clases.
        boolean sePregunto = false;
        boolean mismoPaquete = false;

        if (!Modifier.isPublic(memberClass.getModifiers())) {
            mismoPaquete = Reflection.mismoPaqueteDeClase(currentClass, memberClass);
            sePregunto = true;
            if (!mismoPaquete) {
                return false;
            }
        }

        // Desde aca se sabe que `currentClass` alcanza a `memberClass`; falta el miembro.
        if (Modifier.isPublic(modifiers)) {
            return true;
        }

        // Un `private` cruza la frontera de clase si las dos comparten nido -- que es lo que un nido
        // es. `targetClass` puede quedar afuera y no importa: lo que se accede es el miembro.
        if (Modifier.isPrivate(modifiers)) {
            if (Reflection.areNestMates(currentClass, memberClass)) {
                return true;
            }
        }

        boolean bienHastaAca = false;
        if (Modifier.isProtected(modifiers)) {
            if (Reflection.isSubclassOf(currentClass, memberClass)) {
                bienHastaAca = true;
            }
        }
        if (!bienHastaAca && !Modifier.isPrivate(modifiers)) {
            if (!sePregunto) {
                mismoPaquete = Reflection.mismoPaqueteDeClase(currentClass, memberClass);
                sePregunto = true;
            }
            if (mismoPaquete) {
                bienHastaAca = true;
            }
        }
        if (!bienHastaAca) {
            return false;
        }

        // JLS 6.6.2: heredar un `protected` de otro paquete te deja usarlo sobre TU tipo, no sobre
        // cualquier otro subtipo del que lo declara. Es la regla que impide que una subclase use el
        // `clone()` protegido de `Object` sobre un objeto ajeno.
        if (targetClass != null && Modifier.isProtected(modifiers) && targetClass != currentClass) {
            if (!sePregunto) {
                mismoPaquete = Reflection.mismoPaqueteDeClase(currentClass, memberClass);
                sePregunto = true;
            }
            if (!mismoPaquete) {
                if (!Reflection.isSubclassOf(targetClass, currentClass)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * {@link #verifyMemberAccess} pero tirando en vez de contestar que no.
     *
     * @param currentClass quien accede
     * @param memberClass quien declara el miembro
     * @param targetClass el tipo estatico del receptor, o {@code null}
     * @param modifiers los modificadores del miembro
     * @throws IllegalAccessException si el acceso no es legal
     */
    public static void ensureMemberAccess(Class<?> currentClass, Class<?> memberClass,
                                          Class<?> targetClass, int modifiers)
            throws IllegalAccessException {
        if (!Reflection.verifyMemberAccess(currentClass, memberClass, targetClass, modifiers)) {
            throw Reflection.newIllegalAccessException(currentClass, memberClass, targetClass,
                    modifiers);
        }
    }

    /**
     * El caso barato del chequeo: un miembro publico de un tipo publico en un paquete exportado sin
     * condicion. Sirve para saltearse {@link #verifyMemberAccess} cuando la respuesta no depende de
     * quien pregunte.
     *
     * @param memberClass quien declara el miembro
     * @param modifiers los modificadores del miembro
     * @return si cualquiera puede
     */
    public static boolean verifyPublicMemberAccess(Class<?> memberClass, int modifiers) {
        Module m = memberClass.getModule();
        return Modifier.isPublic(modifiers)
                && m.isExported(memberClass.getPackageName())
                && Modifier.isPublic(memberClass.getModifiers());
    }

    /**
     * Si el modulo de {@code memberClass} le exporta su paquete a {@code currentModule}.
     *
     * <p>En este runtime todo vive en el modulo sin nombre, que exporta todo; la respuesta es siempre
     * que si, y no por atajo sino porque ese <em>es</em> el grafo de modulos que hay.
     *
     * @param currentModule el modulo que accede
     * @param memberClass quien declara el miembro
     * @return si el paquete esta exportado hacia el
     */
    public static boolean verifyModuleAccess(Module currentModule, Class<?> memberClass) {
        Module memberModule = memberClass.getModule();
        if (currentModule == memberModule) {
            return true;
        }
        return memberModule.isExported(memberClass.getPackageName(), currentModule);
    }

    /**
     * La excepcion que describe un acceso ilegal, ya redactada.
     *
     * @param currentClass quien accede
     * @param memberClass quien declara el miembro
     * @param targetClass el tipo estatico del receptor, o {@code null}
     * @param modifiers los modificadores del miembro
     * @return la excepcion, sin tirar
     */
    public static IllegalAccessException newIllegalAccessException(Class<?> currentClass,
                                                                   Class<?> memberClass,
                                                                   Class<?> targetClass,
                                                                   int modifiers) {
        StringBuilder m = new StringBuilder();
        m.append("class ").append(currentClass.getName());
        m.append(" cannot access ");
        String visibilidad = Modifier.isPublic(modifiers) ? "public"
                : Modifier.isProtected(modifiers) ? "protected"
                : Modifier.isPrivate(modifiers) ? "private" : "package-private";
        m.append(visibilidad).append(" member of class ").append(memberClass.getName());
        if (targetClass != null && targetClass != memberClass) {
            m.append(" with modifiers \"").append(Modifier.toString(modifiers)).append('"');
        }
        return new IllegalAccessException(m.toString());
    }

    /**
     * Si las dos clases comparten nido, y por lo tanto los miembros privados una de la otra.
     *
     * <p>En el JDK es un {@code native} porque el nido lo resuelve el VM; aca
     * {@link Class#isNestmateOf} ya hace exactamente esa pregunta contra el mismo nativo, asi que
     * este metodo es el nombre que la maquinaria reflexiva le da a la de alla.
     *
     * @param currentClass una clase
     * @param memberClass la otra
     * @return si estan en el mismo nido
     */
    public static boolean areNestMates(Class<?> currentClass, Class<?> memberClass) {
        return currentClass.isNestmateOf(memberClass);
    }

    // ---- el registro de filtrado ----

    /**
     * Declara que la reflexion no debe entregar esos campos de {@code containingClass}.
     *
     * @param containingClass la clase que los declara
     * @param fieldNames los nombres a esconder, o {@link #ALL_MEMBERS} para todos
     * @throws IllegalArgumentException si esa clase ya tiene un filtro; registrar dos veces
     *                                  <em>reemplazaria</em> el primero, que es como se lo saltearia
     */
    public static synchronized void registerFieldsToFilter(Class<?> containingClass,
                                                           Set<String> fieldNames) {
        Reflection.filtroDeCampos = Reflection.registrar(Reflection.filtroDeCampos, containingClass,
                fieldNames);
    }

    /**
     * Declara que la reflexion no debe entregar esos metodos de {@code containingClass}.
     *
     * @param containingClass la clase que los declara
     * @param methodNames los nombres a esconder, o {@link #ALL_MEMBERS} para todos
     * @throws IllegalArgumentException si esa clase ya tiene un filtro
     */
    public static synchronized void registerMethodsToFilter(Class<?> containingClass,
                                                            Set<String> methodNames) {
        Reflection.filtroDeMetodos = Reflection.registrar(Reflection.filtroDeMetodos,
                containingClass, methodNames);
    }

    /** {@code fields} sin los que {@code containingClass} haya declarado escondidos. */
    public static Field[] filterFields(Class<?> containingClass, Field[] fields) {
        if (Reflection.filtroDeCampos.isEmpty()) {
            return fields;
        }
        return (Field[]) Reflection.filtrar(fields, Reflection.filtroDeCampos.get(containingClass));
    }

    /** {@code methods} sin los que {@code containingClass} haya declarado escondidos. */
    public static Method[] filterMethods(Class<?> containingClass, Method[] methods) {
        if (Reflection.filtroDeMetodos.isEmpty()) {
            return methods;
        }
        return (Method[]) Reflection.filtrar(methods,
                Reflection.filtroDeMetodos.get(containingClass));
    }

    // ---- lo interno ----

    static boolean isSubclassOf(Class<?> queryClass, Class<?> ofClass) {
        Class<?> c = queryClass;
        while (c != null) {
            if (c == ofClass) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    // Dos clases estan en el mismo paquete solo si ademas las cargo el mismo cargador: dos paquetes
    // homonimos de dos cargadores distintos son paquetes distintos, y confundirlos seria justamente
    // la manera de colarse en uno ajeno.
    private static boolean mismoPaqueteDeClase(Class<?> c1, Class<?> c2) {
        if (c1.getClassLoader() != c2.getClassLoader()) {
            return false;
        }
        return Objects.equals(c1.getPackageName(), c2.getPackageName());
    }

    private static Map<Class<?>, Set<String>> registrar(Map<Class<?>, Set<String>> mapa,
                                                        Class<?> c, Set<String> nombres) {
        if (mapa.get(c) != null) {
            throw new IllegalArgumentException("Filter already registered: " + c);
        }
        Map<Class<?>, Set<String>> nuevo = new HashMap<Class<?>, Set<String>>(mapa);
        nuevo.put(c, nombres);
        return nuevo;
    }

    private static Member[] filtrar(Member[] miembros, Set<String> escondidos) {
        if (escondidos == null || miembros.length == 0) {
            return miembros;
        }
        // El arreglo de salida tiene que ser del tipo del de entrada -- `Field[]` o `Method[]` -- y
        // el unico lugar de donde sacarlo sin un parametro `Class` de mas es un elemento.
        Class<?> tipo = miembros[0].getClass();
        if (escondidos.contains(Reflection.COMODIN)) {
            return (Member[]) Array.newInstance(tipo, 0);
        }
        int cuantos = 0;
        int i = 0;
        while (i < miembros.length) {
            if (!escondidos.contains(miembros[i].getName())) {
                cuantos = cuantos + 1;
            }
            i = i + 1;
        }
        Member[] salida = (Member[]) Array.newInstance(tipo, cuantos);
        int destino = 0;
        i = 0;
        while (i < miembros.length) {
            if (!escondidos.contains(miembros[i].getName())) {
                salida[destino] = miembros[i];
                destino = destino + 1;
            }
            i = i + 1;
        }
        return salida;
    }

    /**
     * La clase que llamo al metodo que llama a este.
     *
     * <p>Es un intrinseco de la VM y no un native del puente: hay que mirar <b>los marcos</b>, y el
     * puente solo ve el metaspace y el heap. Devuelve null si no hay tanta pila -- llamarlo desde el
     * metodo de entrada es legitimo y la respuesta correcta ahi es "nadie".
     *
     * <p><b>Diferencia anotada con el JDK</b>: alla este metodo <b>lanza</b>
     * {@code InternalError} si el metodo que lo llama no esta marcado con {@link CallerSensitive}.
     * Aca no. No es una guardia que falte, es una que no hace falta: el JDK la necesita porque su
     * maquinaria de reflexion <b>interpone marcos generados</b> entre el llamador de verdad y el
     * metodo, y su implementacion cuenta profundidad fija; la marca es lo que le dice al runtime que
     * no los interponga. Esta VM no interpone ninguno --{@code Method.invoke} es un intrinseco que
     * empuja el marco del destino y nada mas-- asi que el recorrido da el llamador correcto lo
     * marquen o no.
     *
     * <p>Marcar igual el metodo que lo use sigue siendo lo correcto: es lo que documenta que su
     * respuesta depende de la pila, y es lo que {@link #isCallerSensitive} lee.
     */
    public static native Class<?> getCallerClass();

    /**
     * Los {@code access_flags} <b>crudos</b> del class file.
     *
     * <p>No es lo mismo que {@code Class.getModifiers()}: para una clase anidada, aquel devuelve los
     * modificadores que declara el atributo {@code InnerClasses} --que es donde vive el
     * {@code private} de una clase interna-- y este los del encabezado, donde ese {@code private} no
     * se puede representar. Para decidir accesos hacen falta los crudos.
     */
    public static native int getClassAccessFlags(Class<?> c);

    /**
     * Si ese metodo esta marcado con {@link CallerSensitive}.
     *
     * <p>Se lee del {@code .class} y no de una lista: la marca viaja con el metodo, asi que un
     * metodo sensible al llamador de una biblioteca de terceros se reconoce igual que uno del JDK.
     */
    public static boolean isCallerSensitive(Method m) {
        return m.getAnnotation(CallerSensitive.class) != null;
    }

    /**
     * Si ese campo es un {@code final} en el que se puede <b>confiar</b>: uno que ni siquiera la
     * reflexion con {@code setAccessible} puede escribir.
     *
     * <p>Son dos: los campos de un {@code record} y los de una clase oculta. En los dos casos la
     * inmutabilidad es parte del contrato del tipo --el {@code equals} de un record y el desarme de
     * una lambda dependen de ella-- asi que dejarlos escribir romperia invariantes que el
     * compilador ya dio por buenas al optimizar.
     *
     * <p>Un campo {@code static} nunca lo es: los estaticos se escriben en el {@code <clinit>}, y
     * ahi la escritura es legitima.
     */
    public static boolean isTrustedFinalField(Field f) {
        if (!Modifier.isFinal(f.getModifiers()) || Modifier.isStatic(f.getModifiers())) {
            return false;
        }
        Class<?> owner = f.getDeclaringClass();
        return owner.isRecord() || owner.isHidden();
    }

    /**
     * Comprueba que ese modulo tenga habilitado el acceso nativo.
     *
     * <p>Es el gancho que llaman los metodos restringidos de {@code java.lang.foreign}: en el JDK
     * lanza {@code IllegalCallerException} si el modulo del llamador no arranco con
     * {@code --enable-native-access}.
     *
     * <p><b>Aca no lanza nunca</b>, y hay que decir por que no es un atajo. La bandera existe para
     * que una aplicacion pueda decidir <b>que modulos</b> pueden llamar codigo nativo; esta
     * biblioteca no tiene sistema de modulos, asi que no hay a quien preguntarle ni a quien negarle.
     * Simular una negativa seria inventar una politica; simular una aprobacion --que es lo que hace
     * el JDK cuando el acceso esta habilitado-- es la unica respuesta que se corresponde con el
     * estado real del runtime.
     */
    public static void ensureNativeAccess(Class<?> currentClass, Class<?> owner, String methodName,
            boolean jni) {
    }
}
