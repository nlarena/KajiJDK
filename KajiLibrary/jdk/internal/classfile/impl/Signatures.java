package jdk.internal.classfile.impl;

import java.lang.classfile.ClassSignature;
import java.lang.classfile.MethodSignature;
import java.lang.classfile.Signature;
import java.lang.classfile.Signature.ArrayTypeSig;
import java.lang.classfile.Signature.BaseTypeSig;
import java.lang.classfile.Signature.ClassTypeSig;
import java.lang.classfile.Signature.RefTypeSig;
import java.lang.classfile.Signature.ThrowableSig;
import java.lang.classfile.Signature.TypeArg;
import java.lang.classfile.Signature.TypeArg.Bounded;
import java.lang.classfile.Signature.TypeArg.Bounded.WildcardIndicator;
import java.lang.classfile.Signature.TypeArg.Unbounded;
import java.lang.classfile.Signature.TypeParam;
import java.lang.classfile.Signature.TypeVarSig;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Las firmas genéricas de JVMS §4.7.9.1: el parser, el impresor y los nodos del árbol.
//
// Está todo en una clase con clases anidadas y no en diez archivos por una razón práctica: los nodos
// se construyen unos a otros y desde acá el constructor se escribe sin calificar. Un
// `new Firmas.ClassTypeSigImpl(...)` desde afuera es justo la forma que el compilador de este
// proyecto no resuelve (bug #356), así que no hay ninguna.
//
// El parser es estricto de la única manera que sirve: consume la cadena entera y falla con
// `IllegalArgumentException` ante el primer carácter que la gramática no admite, incluido el sobrante
// al final. Una firma es un dato que viene del archivo; aceptarla a medias es inventar un tipo.
public final class Signatures {

    private Signatures() {
    }

    // ------------------------------------------------------------------ nodos

    /** Un primitivo, o `V` en la posición de resultado. */
    public static final class BaseTypeSigImpl implements BaseTypeSig {

        private final char letter;

        BaseTypeSigImpl(char letter) {
            this.letter = letter;
        }

        public char baseType() {
            return this.letter;
        }

        public String signatureString() {
            return String.valueOf(this.letter);
        }

        public boolean equals(Object o) {
            return o instanceof BaseTypeSigImpl && ((BaseTypeSigImpl) o).letter == this.letter;
        }

        public int hashCode() {
            return this.letter;
        }

        public String toString() {
            return signatureString();
        }
    }

    /** Una clase o interfaz con sus argumentos de tipo. */
    public static final class ClassTypeSigImpl implements ClassTypeSig {

        private final ClassTypeSig outer;
        private final String name;
        private final List<TypeArg> args;

        ClassTypeSigImpl(ClassTypeSig outer, String name, List<TypeArg> args) {
            this.outer = outer;
            this.name = name;
            this.args = args;
        }

        public Optional<ClassTypeSig> outerType() {
            return Optional.ofNullable(this.outer);
        }

        public String className() {
            return this.name;
        }

        public List<TypeArg> typeArgs() {
            return this.args;
        }

        public String signatureString() {
            StringBuilder sb = new StringBuilder();
            if (this.outer == null) {
                sb.append('L').append(this.name);
            } else {
                String s = this.outer.signatureString();
                // El `;` de la externa se reemplaza por el `.` que abre la anidada.
                sb.append(s, 0, s.length() - 1).append('.').append(this.name);
            }
            escribirArgs(sb, this.args);
            return sb.append(';').toString();
        }

        public boolean equals(Object o) {
            if (!(o instanceof ClassTypeSigImpl)) {
                return false;
            }
            return signatureString().equals(((ClassTypeSigImpl) o).signatureString());
        }

        public int hashCode() {
            return signatureString().hashCode();
        }

        public String toString() {
            return signatureString();
        }
    }

    /** Un arreglo de una dimensión sobre su componente. */
    public static final class ArrayTypeSigImpl implements ArrayTypeSig {

        private final Signature component;

        ArrayTypeSigImpl(Signature component) {
            this.component = component;
        }

        public Signature componentSignature() {
            return this.component;
        }

        public String signatureString() {
            return "[" + this.component.signatureString();
        }

        public boolean equals(Object o) {
            return o instanceof ArrayTypeSigImpl
                    && signatureString().equals(((ArrayTypeSigImpl) o).signatureString());
        }

        public int hashCode() {
            return signatureString().hashCode();
        }

        public String toString() {
            return signatureString();
        }
    }

    /** Una variable de tipo. */
    public static final class TypeVarSigImpl implements TypeVarSig {

        private final String id;

        TypeVarSigImpl(String id) {
            this.id = id;
        }

        public String identifier() {
            return this.id;
        }

        public String signatureString() {
            return "T" + this.id + ";";
        }

        public boolean equals(Object o) {
            return o instanceof TypeVarSigImpl && this.id.equals(((TypeVarSigImpl) o).id);
        }

        public int hashCode() {
            return this.id.hashCode();
        }

        public String toString() {
            return signatureString();
        }
    }

    /** La declaración de una variable de tipo con sus cotas. */
    public static final class TypeParamImpl implements TypeParam {

        private final String id;
        private final RefTypeSig classBound;
        private final List<RefTypeSig> interfaceBounds;

        TypeParamImpl(String id, RefTypeSig classBound, List<RefTypeSig> interfaceBounds) {
            this.id = id;
            this.classBound = classBound;
            this.interfaceBounds = interfaceBounds;
        }

        public String identifier() {
            return this.id;
        }

        public Optional<RefTypeSig> classBound() {
            return Optional.ofNullable(this.classBound);
        }

        public List<RefTypeSig> interfaceBounds() {
            return this.interfaceBounds;
        }

        /** El texto de la declaración, o sea `id : cota { : cota }`. */
        public String declaracion() {
            StringBuilder sb = new StringBuilder(this.id);
            sb.append(':');
            if (this.classBound != null) {
                sb.append(this.classBound.signatureString());
            }
            for (int i = 0; i < this.interfaceBounds.size(); i++) {
                sb.append(':').append(this.interfaceBounds.get(i).signatureString());
            }
            return sb.toString();
        }

        public boolean equals(Object o) {
            return o instanceof TypeParamImpl
                    && declaracion().equals(((TypeParamImpl) o).declaracion());
        }

        public int hashCode() {
            return declaracion().hashCode();
        }

        public String toString() {
            return declaracion();
        }
    }

    /** Un argumento de tipo que nombra un tipo, con o sin comodín. */
    public static final class TypeArgBoundedImpl implements Bounded {

        private final WildcardIndicator unbounded;
        private final RefTypeSig type;

        TypeArgBoundedImpl(WildcardIndicator unbounded, RefTypeSig type) {
            this.unbounded = unbounded;
            this.type = type;
        }

        public WildcardIndicator wildcardIndicator() {
            return this.unbounded;
        }

        public RefTypeSig boundType() {
            return this.type;
        }

        public boolean equals(Object o) {
            return o instanceof TypeArgBoundedImpl && toString().equals(o.toString());
        }

        public int hashCode() {
            return toString().hashCode();
        }

        public String toString() {
            return textoDeArg(this);
        }
    }

    /** El argumento `*`. */
    public static final class TypeArgUnboundedImpl implements Unbounded {

        static final TypeArgUnboundedImpl UNICO = new TypeArgUnboundedImpl();

        private TypeArgUnboundedImpl() {
        }

        public String toString() {
            return "*";
        }
    }

    /** La firma de una clase. */
    public static final class ClassSignatureImpl implements ClassSignature {

        private final List<TypeParam> typeParameters;
        private final ClassTypeSig superClase;
        private final List<ClassTypeSig> interfaces;

        ClassSignatureImpl(List<TypeParam> typeParameters, ClassTypeSig superClase,
                List<ClassTypeSig> interfaces) {
            this.typeParameters = typeParameters;
            this.superClase = superClase;
            this.interfaces = interfaces;
        }

        public List<TypeParam> typeParameters() {
            return this.typeParameters;
        }

        public ClassTypeSig superclassSignature() {
            return this.superClase;
        }

        public List<ClassTypeSig> superinterfaceSignatures() {
            return this.interfaces;
        }

        public String signatureString() {
            StringBuilder sb = new StringBuilder();
            escribirParametros(sb, this.typeParameters);
            sb.append(this.superClase.signatureString());
            for (int i = 0; i < this.interfaces.size(); i++) {
                sb.append(this.interfaces.get(i).signatureString());
            }
            return sb.toString();
        }

        public boolean equals(Object o) {
            return o instanceof ClassSignatureImpl
                    && signatureString().equals(((ClassSignatureImpl) o).signatureString());
        }

        public int hashCode() {
            return signatureString().hashCode();
        }

        public String toString() {
            return signatureString();
        }
    }

    /** La firma de un método. */
    public static final class MethodSignatureImpl implements MethodSignature {

        private final List<TypeParam> typeParameters;
        private final List<ThrowableSig> tirados;
        private final Signature resultado;
        private final List<Signature> argumentos;

        MethodSignatureImpl(List<TypeParam> typeParameters, List<ThrowableSig> tirados,
                Signature resultado, List<Signature> argumentos) {
            this.typeParameters = typeParameters;
            this.tirados = tirados;
            this.resultado = resultado;
            this.argumentos = argumentos;
        }

        public List<TypeParam> typeParameters() {
            return this.typeParameters;
        }

        public List<Signature> arguments() {
            return this.argumentos;
        }

        public Signature result() {
            return this.resultado;
        }

        public List<ThrowableSig> throwableSignatures() {
            return this.tirados;
        }

        public String signatureString() {
            StringBuilder sb = new StringBuilder();
            escribirParametros(sb, this.typeParameters);
            sb.append('(');
            for (int i = 0; i < this.argumentos.size(); i++) {
                sb.append(this.argumentos.get(i).signatureString());
            }
            sb.append(')').append(this.resultado.signatureString());
            for (int i = 0; i < this.tirados.size(); i++) {
                sb.append('^').append(this.tirados.get(i).signatureString());
            }
            return sb.toString();
        }

        public boolean equals(Object o) {
            return o instanceof MethodSignatureImpl
                    && signatureString().equals(((MethodSignatureImpl) o).signatureString());
        }

        public int hashCode() {
            return signatureString().hashCode();
        }

        public String toString() {
            return signatureString();
        }
    }

    // ------------------------------------------------------------- impresión

    static String textoDeArg(TypeArg a) {
        if (a instanceof Unbounded) {
            return "*";
        }
        Bounded b = (Bounded) a;
        WildcardIndicator w = b.wildcardIndicator();
        String cuerpo = b.boundType().signatureString();
        if (w == WildcardIndicator.EXTENDS) {
            return "+" + cuerpo;
        }
        if (w == WildcardIndicator.SUPER) {
            return "-" + cuerpo;
        }
        return cuerpo;
    }

    static void escribirArgs(StringBuilder sb, List<TypeArg> args) {
        if (args.isEmpty()) {
            return;
        }
        sb.append('<');
        for (int i = 0; i < args.size(); i++) {
            sb.append(textoDeArg(args.get(i)));
        }
        sb.append('>');
    }

    static void escribirParametros(StringBuilder sb, List<TypeParam> ps) {
        if (ps.isEmpty()) {
            return;
        }
        sb.append('<');
        for (int i = 0; i < ps.size(); i++) {
            TypeParam p = ps.get(i);
            sb.append(p.identifier()).append(':');
            Optional<RefTypeSig> cota = p.classBound();
            if (cota.isPresent()) {
                sb.append(cota.get().signatureString());
            }
            List<RefTypeSig> ifs = p.interfaceBounds();
            for (int j = 0; j < ifs.size(); j++) {
                sb.append(':').append(ifs.get(j).signatureString());
            }
        }
        sb.append('>');
    }

    // ------------------------------------------------------------- fábricas

    private static final BaseTypeSigImpl[] PRIMITIVOS = new BaseTypeSigImpl[128];

    /** El nodo del primitivo cuya letra es `letra`. */
    public static BaseTypeSig baseTypeSig(char letter) {
        if ("BCDFIJSZV".indexOf(letter) < 0) {
            throw new IllegalArgumentException("no es una letra de tipo base: " + letter);
        }
        synchronized (PRIMITIVOS) {
            BaseTypeSigImpl b = PRIMITIVOS[letter];
            if (b == null) {
                b = new BaseTypeSigImpl(letter);
                PRIMITIVOS[letter] = b;
            }
            return b;
        }
    }

    /** El nodo de la clase `nombre` (nombre interno, o simple si va anidada) con estos argumentos. */
    public static ClassTypeSig classTypeSig(ClassTypeSig outer, String name,
            TypeArg... args) {
        exigirNombreDeClase(name);
        List<TypeArg> lista = new ArrayList<TypeArg>();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    throw new NullPointerException("typeArgs[" + i + "]");
                }
                lista.add(args[i]);
            }
        }
        return new ClassTypeSigImpl(outer, name, Collections.unmodifiableList(lista));
    }

    /** El nodo del arreglo de `dims` dimensiones sobre `componente`. */
    public static ArrayTypeSig arrayTypeSig(int dims, Signature component) {
        if (component == null) {
            throw new NullPointerException("componentSignature");
        }
        if (dims < 1 || dims > 255) {
            throw new IllegalArgumentException("dimensiones fuera de rango: " + dims);
        }
        if (component instanceof BaseTypeSig && ((BaseTypeSig) component).baseType() == 'V') {
            throw new IllegalArgumentException("no hay arreglos de void");
        }
        Signature s = component;
        for (int i = 0; i < dims; i++) {
            s = new ArrayTypeSigImpl(s);
        }
        return (ArrayTypeSig) s;
    }

    /** El nodo de la variable de tipo `id`. */
    public static TypeVarSig typeVarSig(String id) {
        exigirIdentificador(id);
        return new TypeVarSigImpl(id);
    }

    /** La declaración de la variable `id` con estas cotas. */
    public static TypeParam typeParam(String id, Optional<RefTypeSig> classBound,
            RefTypeSig... interfaceBounds) {
        exigirIdentificador(id);
        if (classBound == null) {
            throw new NullPointerException("classBound");
        }
        List<RefTypeSig> ifs = new ArrayList<RefTypeSig>();
        if (interfaceBounds != null) {
            for (int i = 0; i < interfaceBounds.length; i++) {
                if (interfaceBounds[i] == null) {
                    throw new NullPointerException("interfaceBounds[" + i + "]");
                }
                ifs.add(interfaceBounds[i]);
            }
        }
        return new TypeParamImpl(id, classBound.orElse(null),
                Collections.unmodifiableList(ifs));
    }

    /** El argumento `*`. */
    public static Unbounded unbounded() {
        return TypeArgUnboundedImpl.UNICO;
    }

    /** El argumento con este comodín sobre este tipo. */
    public static Bounded bounded(WildcardIndicator unbounded, RefTypeSig type) {
        if (unbounded == null) {
            throw new NullPointerException("wildcardIndicator");
        }
        if (type == null) {
            throw new NullPointerException("boundType");
        }
        return new TypeArgBoundedImpl(unbounded, type);
    }

    /** La firma sin genéricos del tipo que describe `desc`. */
    public static Signature ofDescriptor(ClassDesc desc) {
        if (desc == null) {
            throw new NullPointerException("classDesc");
        }
        if (desc.isPrimitive()) {
            return baseTypeSig(desc.descriptorString().charAt(0));
        }
        if (desc.isArray()) {
            return new ArrayTypeSigImpl(ofDescriptor(desc.componentType()));
        }
        String d = desc.descriptorString();
        return classTypeSig(null, d.substring(1, d.length() - 1));
    }

    /** La firma de clase con estos parámetros (nulo = ninguno), superclase e interfaces. */
    public static ClassSignature classSignature(List<TypeParam> typeParameters,
            ClassTypeSig superClase, ClassTypeSig... interfaces) {
        if (superClase == null) {
            throw new NullPointerException("superclassSignature");
        }
        List<ClassTypeSig> ifs = new ArrayList<ClassTypeSig>();
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length; i++) {
                if (interfaces[i] == null) {
                    throw new NullPointerException("superinterfaceSignatures[" + i + "]");
                }
                ifs.add(interfaces[i]);
            }
        }
        return new ClassSignatureImpl(congelarParametros(typeParameters), superClase,
                Collections.unmodifiableList(ifs));
    }

    /** La firma de método con estas partes; `parametros` y `tirados` pueden ser nulos. */
    public static MethodSignature methodSignature(List<TypeParam> typeParameters,
            List<ThrowableSig> tirados, Signature resultado, Signature... argumentos) {
        if (resultado == null) {
            throw new NullPointerException("result");
        }
        List<Signature> args = new ArrayList<Signature>();
        if (argumentos != null) {
            for (int i = 0; i < argumentos.length; i++) {
                if (argumentos[i] == null) {
                    throw new NullPointerException("arguments[" + i + "]");
                }
                args.add(argumentos[i]);
            }
        }
        List<ThrowableSig> ts = new ArrayList<ThrowableSig>();
        if (tirados != null) {
            for (int i = 0; i < tirados.size(); i++) {
                ThrowableSig t = tirados.get(i);
                if (t == null) {
                    throw new NullPointerException("exceptions[" + i + "]");
                }
                ts.add(t);
            }
        }
        return new MethodSignatureImpl(congelarParametros(typeParameters),
                Collections.unmodifiableList(ts), resultado,
                Collections.unmodifiableList(args));
    }

    /** La firma sin genéricos del método que describe `desc`. */
    public static MethodSignature methodSignatureOf(MethodTypeDesc desc) {
        if (desc == null) {
            throw new NullPointerException("descriptor");
        }
        List<Signature> args = new ArrayList<Signature>();
        for (int i = 0; i < desc.parameterCount(); i++) {
            args.add(ofDescriptor(desc.parameterType(i)));
        }
        return new MethodSignatureImpl(Collections.<TypeParam>emptyList(),
                Collections.<ThrowableSig>emptyList(), ofDescriptor(desc.returnType()),
                Collections.unmodifiableList(args));
    }

    private static List<TypeParam> congelarParametros(List<TypeParam> ps) {
        List<TypeParam> lista = new ArrayList<TypeParam>();
        if (ps != null) {
            for (int i = 0; i < ps.size(); i++) {
                TypeParam p = ps.get(i);
                if (p == null) {
                    throw new NullPointerException("typeParameters[" + i + "]");
                }
                lista.add(p);
            }
        }
        return Collections.unmodifiableList(lista);
    }

    // --------------------------------------------------------------- parseo

    /** El cursor sobre el texto de una firma. */
    static final class Analizador {

        final String s;
        int i;

        Analizador(String s) {
            if (s == null) {
                throw new NullPointerException("signature");
            }
            this.s = s;
            this.i = 0;
        }

        boolean hay() {
            return this.i < this.s.length();
        }

        char mirar() {
            if (!hay()) {
                throw error("se acabo la firma");
            }
            return this.s.charAt(this.i);
        }

        char tomar() {
            char c = mirar();
            this.i++;
            return c;
        }

        void exigir(char c) {
            char v = tomar();
            if (v != c) {
                this.i--;
                throw error("se esperaba '" + c + "' y hay '" + v + "'");
            }
        }

        IllegalArgumentException error(String que) {
            return new IllegalArgumentException(
                    que + " en la posicion " + this.i + " de: " + this.s);
        }

        /** Lee hasta el primero de `cortes`, sin consumirlo. */
        String identificadorHasta(String cortes) {
            int desde = this.i;
            while (hay() && cortes.indexOf(this.s.charAt(this.i)) < 0) {
                this.i++;
            }
            if (this.i == desde) {
                throw error("identificador vacio");
            }
            return this.s.substring(desde, this.i);
        }

        Signature type() {
            char c = mirar();
            // La `V` entra aca y no solo en la posicion de resultado: es lo que hace el JDK, y hay
            // una prueba diferencial que lo fija.
            if ("BCDFIJSZV".indexOf(c) >= 0) {
                this.i++;
                return baseTypeSig(c);
            }
            return tipoDeReferencia();
        }

        RefTypeSig tipoDeReferencia() {
            char c = mirar();
            if (c == 'L') {
                return claseSig();
            }
            if (c == 'T') {
                this.i++;
                String id = identificadorHasta(";");
                exigir(';');
                return new TypeVarSigImpl(id);
            }
            if (c == '[') {
                this.i++;
                return new ArrayTypeSigImpl(type());
            }
            throw error("no arranca un tipo de referencia: '" + c + "'");
        }

        ClassTypeSig claseSig() {
            exigir('L');
            ClassTypeSig actual = null;
            while (true) {
                String name = identificadorHasta("<;.");
                List<TypeArg> args = argumentos();
                actual = new ClassTypeSigImpl(actual, name,
                        Collections.unmodifiableList(args));
                char c = tomar();
                if (c == ';') {
                    return actual;
                }
                if (c != '.') {
                    this.i--;
                    throw error("se esperaba '.' o ';' y hay '" + c + "'");
                }
            }
        }

        List<TypeArg> argumentos() {
            List<TypeArg> args = new ArrayList<TypeArg>();
            if (!hay() || mirar() != '<') {
                return args;
            }
            this.i++;
            if (mirar() == '>') {
                throw error("lista de argumentos de tipo vacia");
            }
            while (mirar() != '>') {
                char c = mirar();
                if (c == '*') {
                    this.i++;
                    args.add(TypeArgUnboundedImpl.UNICO);
                } else if (c == '+') {
                    this.i++;
                    args.add(new TypeArgBoundedImpl(WildcardIndicator.EXTENDS,
                            tipoDeReferencia()));
                } else if (c == '-') {
                    this.i++;
                    args.add(new TypeArgBoundedImpl(WildcardIndicator.SUPER,
                            tipoDeReferencia()));
                } else {
                    args.add(new TypeArgBoundedImpl(WildcardIndicator.NONE,
                            tipoDeReferencia()));
                }
            }
            this.i++;
            return args;
        }

        List<TypeParam> typeParameters() {
            List<TypeParam> ps = new ArrayList<TypeParam>();
            if (!hay() || mirar() != '<') {
                return ps;
            }
            this.i++;
            if (mirar() == '>') {
                throw error("lista de parametros de tipo vacia");
            }
            while (mirar() != '>') {
                String id = identificadorHasta(":");
                exigir(':');
                RefTypeSig classBound = null;
                if (mirar() != ':') {
                    classBound = tipoDeReferencia();
                }
                List<RefTypeSig> ifs = new ArrayList<RefTypeSig>();
                while (hay() && mirar() == ':') {
                    this.i++;
                    ifs.add(tipoDeReferencia());
                }
                ps.add(new TypeParamImpl(id, classBound, Collections.unmodifiableList(ifs)));
            }
            this.i++;
            return ps;
        }

        void exigirFin() {
            if (hay()) {
                throw error("sobra texto despues de la firma");
            }
        }
    }

    /** Parsea una firma de tipo completa. */
    public static Signature parseTipo(String texto) {
        Analizador a = new Analizador(texto);
        Signature s = a.type();
        a.exigirFin();
        return s;
    }

    /** Parsea una firma de clase completa. */
    public static ClassSignature parseClassSignature(String texto) {
        Analizador a = new Analizador(texto);
        List<TypeParam> ps = a.typeParameters();
        ClassTypeSig sup = a.claseSig();
        List<ClassTypeSig> ifs = new ArrayList<ClassTypeSig>();
        while (a.hay()) {
            ifs.add(a.claseSig());
        }
        return new ClassSignatureImpl(Collections.unmodifiableList(ps), sup,
                Collections.unmodifiableList(ifs));
    }

    /** Parsea una firma de método completa. */
    public static MethodSignature parseMethodSignature(String texto) {
        Analizador a = new Analizador(texto);
        List<TypeParam> ps = a.typeParameters();
        a.exigir('(');
        List<Signature> args = new ArrayList<Signature>();
        while (a.mirar() != ')') {
            args.add(a.type());
        }
        a.exigir(')');
        Signature resultado = a.type();
        List<ThrowableSig> tirados = new ArrayList<ThrowableSig>();
        while (a.hay()) {
            a.exigir('^');
            char c = a.mirar();
            if (c == 'L') {
                tirados.add(a.claseSig());
            } else if (c == 'T') {
                a.tomar();
                String id = a.identificadorHasta(";");
                a.exigir(';');
                tirados.add(new TypeVarSigImpl(id));
            } else {
                throw a.error("un `throws` solo admite una clase o una variable de tipo");
            }
        }
        return new MethodSignatureImpl(Collections.unmodifiableList(ps),
                Collections.unmodifiableList(tirados), resultado,
                Collections.unmodifiableList(args));
    }

    // ------------------------------------------------------------ validación

    private static void exigirIdentificador(String id) {
        if (id == null) {
            throw new NullPointerException("identifier");
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("identificador vacio");
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (".;[/<>:".indexOf(c) >= 0) {
                throw new IllegalArgumentException(
                        "un identificador no puede tener '" + c + "': " + id);
            }
        }
    }

    private static void exigirNombreDeClase(String name) {
        if (name == null) {
            throw new NullPointerException("className");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("nombre de clase vacio");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (".;[<>:".indexOf(c) >= 0) {
                throw new IllegalArgumentException(
                        "un nombre interno no puede tener '" + c + "': " + name);
            }
        }
    }
}
