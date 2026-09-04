import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.function.Consumer;

/**
 * `java.lang.classfile`: escribir un `.class` y volver a leerlo.
 *
 * <p>Lo que se comprueba no es que los metodos existan sino que los **bytes** que salen son un
 * `.class` de verdad: se arma una clase con el constructor, se la vuelve a parsear con el lector, y
 * se pregunta por lo que se le puso. Un escritor que emita basura pasa el compilador y falla aca.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25, corriendo SU `java.lang.classfile`. Eso lo
 * vuelve un oraculo: si nuestro escritor y el de alla no coinciden en lo que el lector encuentra, la
 * prueba lo dice.
 *
 * <p>Y hay una comprobacion mas fuerte que esta prueba no puede hacer sola: `main` deja el `.class`
 * escrito en disco para que una JVM real lo cargue y lo corra. Eso es lo unico que demuestra que los
 * bytes sirven; el resto es nuestro lector leyendo a nuestro escritor.
 */
public class CfBuildTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    /**
     * Los descriptores que la prueba usa.
     *
     * <p>Estan en un objeto y no en constantes `static final`, y **eso no es estilo**: en el JDK
     * 25.0.2, construir un `ClassDesc` de un primitivo desde el inicializador estatico de una clase
     * tira `ExceptionInInitializerError`. `PrimitiveClassDescImpl.<clinit>` llama por dentro a
     * `MethodTypeDescImpl` cuando ese todavia no se inicializo, y la circularidad solo se dispara
     * cuando la inicializacion esta anidada dentro de otra -- desde `main` anda. Se comprobo con un
     * caso de cuatro lineas contra `java` de verdad. Es un problema del JDK; esta prueba lo usa de
     * oraculo, asi que lo esquiva.
     */
    static final class Descs {

        // Los primitivos salen de `ConstantDescs` y no de `ClassDesc.ofDescriptor`, y **eso no es
        // preferencia**: en el JDK 25.0.2, un `ClassDesc.ofDescriptor("I")` como primer uso de
        // `java.lang.constant` deja el paquete inutilizable --`NoClassDefFoundError: Could not
        // initialize class ConstantDescs`, con un NPE adentro de `PrimitiveClassDescImpl.<clinit>`--.
        // Entrando por `ConstantDescs`, su inicializador corre en el orden que el mismo espera y
        // todo anda. Se comprobo con dos casos de cuatro lineas contra `java` de verdad. Es un
        // problema del JDK, no nuestro; esta prueba lo usa de oraculo y tiene que poder correr alla.
        final ClassDesc object = java.lang.constant.ConstantDescs.CD_Object;
        final ClassDesc int_ = java.lang.constant.ConstantDescs.CD_int;
        final ClassDesc void_ = java.lang.constant.ConstantDescs.CD_void;
        final ClassDesc hecha = ClassDesc.of("Hecha");
        final MethodTypeDesc mtInt = MethodTypeDesc.of(this.int_);
        final MethodTypeDesc mtVoid = MethodTypeDesc.of(this.void_);
        final MethodTypeDesc mtIntInt = MethodTypeDesc.of(this.int_, this.int_);
    }

    /** El cuerpo de `run()`: devuelve 42 sin saltos, asi que no necesita mapa de pila. */
    static final class Run implements Consumer<CodeBuilder> {

        public void accept(CodeBuilder cb) {
            cb.loadConstant(42);
            cb.ireturn();
        }
    }

    /** El cuerpo de `doble(int)`: multiplica por dos. Tampoco tiene saltos. */
    static final class Doble implements Consumer<CodeBuilder> {

        public void accept(CodeBuilder cb) {
            cb.iload(0);
            cb.iconst_2();
            cb.imul();
            cb.ireturn();
        }
    }

    /** El constructor vacio: `super()` y volver. */
    static final class Ctor implements Consumer<CodeBuilder> {

        private final Descs d;

        Ctor(Descs d) {
            this.d = d;
        }

        public void accept(CodeBuilder cb) {
            cb.aload(0);
            cb.invokespecial(this.d.object, "<init>", this.d.mtVoid);
            cb.return_();
        }
    }

    /** La clase entera. */
    static final class Cuerpo implements Consumer<ClassBuilder> {

        private final Descs d;

        Cuerpo(Descs d) {
            this.d = d;
        }

        public void accept(ClassBuilder cb) {
            cb.withVersion(52, 0);
            cb.withFlags(0x0021); // public super
            cb.withSuperclass(this.d.object);
            cb.withField("n", this.d.int_, 0x0009); // public static
            cb.withMethodBody("<init>", this.d.mtVoid, 0x0001, new Ctor(this.d));
            cb.withMethodBody("run", this.d.mtInt, 0x0009, new Run());
            cb.withMethodBody("doble", this.d.mtIntInt, 0x0009, new Doble());
        }
    }

    /** Los bytes de la clase de prueba. */
    static byte[] construir() {
        Descs d = new Descs();
        return ClassFile.of().build(d.hecha, new Cuerpo(d));
    }

    /** Una transformacion que le saca el campo y deja todo lo demas. */
    static final class SinCampos implements ClassTransform {

        public void accept(ClassBuilder cb, ClassElement e) {
            if (!(e instanceof FieldModel)) {
                cb.with(e);
            }
        }
    }

    public static int run() {
        failures = 0;

        byte[] bytes = CfBuildTest.construir();
        ok("salieron bytes", bytes != null && bytes.length > 0);
        // El magic, que es lo primero que mira cualquier JVM.
        ok("empieza con CAFEBABE", bytes[0] == (byte) 0xCA && bytes[1] == (byte) 0xFE
                && bytes[2] == (byte) 0xBA && bytes[3] == (byte) 0xBE);

        ClassModel m = ClassFile.of().parse(bytes);
        ok("se llama Hecha", "Hecha".equals(m.thisClass().asInternalName()));
        ok("la version es la que se pidio", m.majorVersion() == 52 && m.minorVersion() == 0);
        ok("las banderas son las que se pidieron", m.flags().flagsMask() == 0x0021);
        ok("hereda de Object", m.superclass().isPresent()
                && "java/lang/Object".equals(m.superclass().get().asInternalName()));
        ok("no implementa nada", m.interfaces().isEmpty());

        ok("tiene un campo", m.fields().size() == 1);
        FieldModel f = m.fields().get(0);
        ok("el campo se llama n", "n".equals(f.fieldName().stringValue()));
        ok("el campo es int", "I".equals(f.fieldType().stringValue()));
        ok("el campo es public static", f.flags().flagsMask() == 0x0009);

        ok("tiene tres metodos", m.methods().size() == 3);
        MethodModel run = CfBuildTest.buscar(m, "run");
        ok("esta run()", run != null);
        if (run != null) {
            ok("run devuelve int", "()I".equals(run.methodType().stringValue()));
            ok("run es public static", run.flags().flagsMask() == 0x0009);
            ok("run tiene cuerpo", run.code().isPresent());
            if (run.code().isPresent()) {
                CodeModel c = run.code().get();
                // Dos instrucciones: la constante y el retorno. Si el escritor emitio algo de mas o
                // de menos, aca se ve.
                int n = 0;
                for (java.lang.classfile.CodeElement e : c) {
                    if (e instanceof java.lang.classfile.Instruction) {
                        n = n + 1;
                    }
                }
                ok("run tiene dos instrucciones", n == 2);
            }
        }

        MethodModel doble = CfBuildTest.buscar(m, "doble");
        ok("esta doble(int)", doble != null);
        if (doble != null) {
            ok("doble toma y devuelve int", "(I)I".equals(doble.methodType().stringValue()));
        }

        // ---- la verificacion estructural no encuentra nada que objetar
        ok("verify no encuentra errores", ClassFile.of().verify(bytes).isEmpty());

        // ---- transformar: la copia sin el campo
        byte[] sinCampos = ClassFile.of().transformClass(m, new SinCampos());
        ClassModel m2 = ClassFile.of().parse(sinCampos);
        ok("la copia no tiene campos", m2.fields().isEmpty());
        ok("la copia conserva los metodos", m2.methods().size() == 3);
        ok("la copia conserva el nombre", "Hecha".equals(m2.thisClass().asInternalName()));
        ok("la copia conserva la version", m2.majorVersion() == 52);

        // ---- la transformacion que no cambia nada deja todo igual
        byte[] igual = ClassFile.of().transformClass(m, ClassTransform.ACCEPT_ALL);
        ClassModel m3 = ClassFile.of().parse(igual);
        ok("ACCEPT_ALL conserva los campos", m3.fields().size() == 1);
        ok("ACCEPT_ALL conserva los metodos", m3.methods().size() == 3);

        // ---- encadenar dos transformaciones
        ClassTransform dos = new SinCampos().andThen(ClassTransform.ACCEPT_ALL);
        ClassModel m4 = ClassFile.of().parse(ClassFile.of().transformClass(m, dos));
        ok("la cadena aplica la primera", m4.fields().isEmpty());
        ok("la cadena no pierde los metodos", m4.methods().size() == 3);

        // ---- las constantes del formato
        ok("latestMajorVersion es 69", ClassFile.latestMajorVersion() == 69);

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    static MethodModel buscar(ClassModel m, String name) {
        for (int i = 0; i < m.methods().size(); i++) {
            if (name.equals(m.methods().get(i).methodName().stringValue())) {
                return m.methods().get(i);
            }
        }
        return null;
    }

    /**
     * Ademas de correr las comprobaciones, deja el `.class` en disco.
     *
     * <p>Es lo que permite la prueba que de verdad importa: `java -cp <dir> Hecha` cargandolo con
     * una JVM real. Nuestro lector leyendo a nuestro escritor puede estar de acuerdo en un error.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("CfBuildTest " + CfBuildTest.run());
        if (args.length > 0) {
            java.io.FileOutputStream out = new java.io.FileOutputStream(args[0]);
            out.write(CfBuildTest.construir());
            out.close();
            System.out.println("escrito " + args[0]);
        }
    }
}
