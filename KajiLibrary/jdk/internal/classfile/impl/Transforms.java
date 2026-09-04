package jdk.internal.classfile.impl;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.FieldBuilder;
import java.lang.classfile.FieldElement;
import java.lang.classfile.FieldModel;
import java.lang.classfile.FieldTransform;
import java.lang.classfile.Label;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.classfile.TypeKind;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Las implementaciones de las fábricas de las cuatro transformaciones.
 *
 * <p>Están las cuatro juntas y no una por archivo porque son la **misma** clase escrita cuatro
 * veces: encadenar, filtrar, agregar al final y dar estado no dependen de qué se transforma. La
 * repetición es del sistema de tipos, no del problema — `ClassTransform` y `MethodTransform` no
 * tienen supertipo común que fije `E` y `B`, así que no hay forma de escribir una sola.
 *
 * <h2>El encadenado, que es lo único no evidente</h2>
 *
 * <p>`a.andThen(b)` **no** corre las dos sobre el original: corre `b` sobre lo que `a` produce. Para
 * eso, `a` no puede escribir en el constructor de verdad — tiene que escribir en uno intermedio que
 * le pase cada elemento a `b`. Eso es lo que son las clases `Chained*Builder` de más abajo: un
 * constructor que por dentro es una transformación.
 */
public final class Transforms {

    private Transforms() {
    }

    // ---- el predicado que acepta todo ----------------------------------------------------------

    /** El predicado de métodos que dice que sí a todos. */
    public static Predicate<MethodModel> allMethods() {
        return AllMethods.INSTANCE;
    }

    // ---- clase ---------------------------------------------------------------------------------

    /** Una transformación de clase seguida de otra. */
    public static ClassTransform chainClass(ClassTransform first, ClassTransform second) {
        return new ChainedClass(first, second);
    }

    /** La que tira lo que cumple el predicado. */
    public static ClassTransform droppingClass(Predicate<ClassElement> filter) {
        return new DroppingClass(filter);
    }

    /** La que deja pasar todo y corre eso al final. */
    public static ClassTransform endHandlerClass(Consumer<ClassBuilder> finisher) {
        return new EndHandlerClass(finisher);
    }

    /** La que se fabrica de nuevo por cada uso. */
    public static ClassTransform statefulClass(Supplier<ClassTransform> supplier) {
        return new StatefulClass(supplier);
    }

    /** La que transforma cada campo. */
    public static ClassTransform transformingFields(FieldTransform xform) {
        return new TransformingFields(xform);
    }

    /** La que transforma los métodos que cumplen el predicado. */
    public static ClassTransform transformingMethods(Predicate<MethodModel> filter,
            MethodTransform xform) {
        return new TransformingMethods(filter, xform);
    }

    // ---- método --------------------------------------------------------------------------------

    /** Una transformación de método seguida de otra. */
    public static MethodTransform chainMethod(MethodTransform first, MethodTransform second) {
        return new ChainedMethod(first, second);
    }

    /** La que tira lo que cumple el predicado. */
    public static MethodTransform droppingMethod(Predicate<MethodElement> filter) {
        return new DroppingMethod(filter);
    }

    /** La que deja pasar todo y corre eso al final. */
    public static MethodTransform endHandlerMethod(Consumer<MethodBuilder> finisher) {
        return new EndHandlerMethod(finisher);
    }

    /** La que se fabrica de nuevo por cada uso. */
    public static MethodTransform statefulMethod(Supplier<MethodTransform> supplier) {
        return new StatefulMethod(supplier);
    }

    /** La que transforma el cuerpo. */
    public static MethodTransform transformingCode(CodeTransform xform) {
        return new TransformingCode(xform);
    }

    // ---- campo ---------------------------------------------------------------------------------

    /** Una transformación de campo seguida de otra. */
    public static FieldTransform chainField(FieldTransform first, FieldTransform second) {
        return new ChainedField(first, second);
    }

    /** La que tira lo que cumple el predicado. */
    public static FieldTransform droppingField(Predicate<FieldElement> filter) {
        return new DroppingField(filter);
    }

    /** La que deja pasar todo y corre eso al final. */
    public static FieldTransform endHandlerField(Consumer<FieldBuilder> finisher) {
        return new EndHandlerField(finisher);
    }

    /** La que se fabrica de nuevo por cada uso. */
    public static FieldTransform statefulField(Supplier<FieldTransform> supplier) {
        return new StatefulField(supplier);
    }

    // ---- código --------------------------------------------------------------------------------

    /** Una transformación de código seguida de otra. */
    public static CodeTransform chainCode(CodeTransform first, CodeTransform second) {
        return new ChainedCode(first, second);
    }

    /** La que deja pasar todo y corre eso al final. */
    public static CodeTransform endHandlerCode(Consumer<CodeBuilder> finisher) {
        return new EndHandlerCode(finisher);
    }

    /** La que se fabrica de nuevo por cada uso. */
    public static CodeTransform statefulCode(Supplier<CodeTransform> supplier) {
        return new StatefulCode(supplier);
    }

    /**
     * Un constructor de codigo que escribe **a traves** de esa transformacion.
     *
     * <p>Lo pide `CodeBuilder.transforming`. Esta fabrica existe porque la clase es de paquete y el
     * que la necesita esta en otro archivo del mismo paquete pero no puede nombrarla desde la
     * interfaz publica.
     */
    public static CodeBuilder chainedCodeBuilder(CodeBuilder downstream, CodeTransform transform) {
        return new ChainedCodeBuilder(downstream, transform);
    }
}

final class AllMethods implements Predicate<MethodModel> {

    static final AllMethods INSTANCE = new AllMethods();

    public boolean test(MethodModel m) {
        return true;
    }
}

// ---- clase -------------------------------------------------------------------------------------

final class ChainedClass implements ClassTransform {

    private final ClassTransform first;
    private final ClassTransform second;

    ChainedClass(ClassTransform first, ClassTransform second) {
        this.first = first;
        this.second = second;
    }

    public void accept(ClassBuilder builder, ClassElement element) {
        this.first.accept(new ChainedClassBuilder(builder, this.second), element);
    }

    public void atStart(ClassBuilder builder) {
        ClassBuilder inner = new ChainedClassBuilder(builder, this.second);
        this.first.atStart(inner);
        this.second.atStart(builder);
    }

    // El orden es el inverso al de `atStart`, y hace falta: lo que la primera escriba en su cierre
    // todavía tiene que pasar por la segunda, así que la segunda cierra después.
    public void atEnd(ClassBuilder builder) {
        this.first.atEnd(new ChainedClassBuilder(builder, this.second));
        this.second.atEnd(builder);
    }
}

// Un `ClassBuilder` que por dentro es una transformación: lo que se le escribe no va al constructor
// de destino sino al `accept` de la transformación, con el destino como salida.
final class ChainedClassBuilder implements ClassBuilder {

    private final ClassBuilder downstream;
    private final ClassTransform transform;

    ChainedClassBuilder(ClassBuilder downstream, ClassTransform transform) {
        this.downstream = downstream;
        this.transform = transform;
    }

    public ClassBuilder with(ClassElement e) {
        this.transform.accept(this.downstream, e);
        return this;
    }

    public ConstantPoolBuilder constantPool() {
        return this.downstream.constantPool();
    }

    // Los cuatro de abajo van derecho al destino y **no** pasan por la transformación. No es una
    // omisión: la transformación trabaja sobre elementos, y un campo o un método que se crea con
    // `withField`/`withMethod` no es un elemento que alguien haya emitido -- es una estructura nueva
    // que quien la crea ya decidió cómo quiere.
    public ClassBuilder withField(Utf8Entry name, Utf8Entry descriptor,
            Consumer<FieldBuilder> handler) {
        this.downstream.withField(name, descriptor, handler);
        return this;
    }

    public ClassBuilder transformField(FieldModel field, FieldTransform xform) {
        this.downstream.transformField(field, xform);
        return this;
    }

    public ClassBuilder withMethod(Utf8Entry name, Utf8Entry descriptor, int flags,
            Consumer<MethodBuilder> handler) {
        this.downstream.withMethod(name, descriptor, flags, handler);
        return this;
    }

    public ClassBuilder transformMethod(MethodModel method, MethodTransform xform) {
        this.downstream.transformMethod(method, xform);
        return this;
    }
}

final class DroppingClass implements ClassTransform {

    private final Predicate<ClassElement> filter;

    DroppingClass(Predicate<ClassElement> filter) {
        this.filter = filter;
    }

    public void accept(ClassBuilder builder, ClassElement element) {
        if (!this.filter.test(element)) {
            builder.with(element);
        }
    }
}

final class EndHandlerClass implements ClassTransform {

    private final Consumer<ClassBuilder> finisher;

    EndHandlerClass(Consumer<ClassBuilder> finisher) {
        this.finisher = finisher;
    }

    public void accept(ClassBuilder builder, ClassElement element) {
        builder.with(element);
    }

    public void atEnd(ClassBuilder builder) {
        this.finisher.accept(builder);
    }
}

// La transformación con estado. Pide una nueva al proveedor en `atStart` y la usa hasta `atEnd`.
//
// El campo mutable es exactamente lo que esta clase existe para encapsular: una transformación con
// estado no se puede compartir, y guardarlo acá --con una instancia nueva por aplicación-- es lo que
// hace que quien la use no tenga que saberlo.
final class StatefulClass implements ClassTransform {

    private final Supplier<ClassTransform> supplier;
    private ClassTransform current;

    StatefulClass(Supplier<ClassTransform> supplier) {
        this.supplier = supplier;
    }

    public void atStart(ClassBuilder builder) {
        this.current = this.supplier.get();
        this.current.atStart(builder);
    }

    public void accept(ClassBuilder builder, ClassElement element) {
        this.current.accept(builder, element);
    }

    public void atEnd(ClassBuilder builder) {
        this.current.atEnd(builder);
        this.current = null;
    }
}

final class TransformingFields implements ClassTransform {

    private final FieldTransform xform;

    TransformingFields(FieldTransform xform) {
        this.xform = xform;
    }

    public void accept(ClassBuilder builder, ClassElement element) {
        if (element instanceof FieldModel) {
            builder.transformField((FieldModel) element, this.xform);
        } else {
            builder.with(element);
        }
    }
}

final class TransformingMethods implements ClassTransform {

    private final Predicate<MethodModel> filter;
    private final MethodTransform xform;

    TransformingMethods(Predicate<MethodModel> filter, MethodTransform xform) {
        this.filter = filter;
        this.xform = xform;
    }

    public void accept(ClassBuilder builder, ClassElement element) {
        if (element instanceof MethodModel && this.filter.test((MethodModel) element)) {
            builder.transformMethod((MethodModel) element, this.xform);
        } else {
            builder.with(element);
        }
    }
}

// ---- método --------------------------------------------------------------------------------------

final class ChainedMethod implements MethodTransform {

    private final MethodTransform first;
    private final MethodTransform second;

    ChainedMethod(MethodTransform first, MethodTransform second) {
        this.first = first;
        this.second = second;
    }

    public void accept(MethodBuilder builder, MethodElement element) {
        this.first.accept(new ChainedMethodBuilder(builder, this.second), element);
    }

    public void atStart(MethodBuilder builder) {
        this.first.atStart(new ChainedMethodBuilder(builder, this.second));
        this.second.atStart(builder);
    }

    public void atEnd(MethodBuilder builder) {
        this.first.atEnd(new ChainedMethodBuilder(builder, this.second));
        this.second.atEnd(builder);
    }
}

final class ChainedMethodBuilder implements MethodBuilder {

    private final MethodBuilder downstream;
    private final MethodTransform transform;

    ChainedMethodBuilder(MethodBuilder downstream, MethodTransform transform) {
        this.downstream = downstream;
        this.transform = transform;
    }

    public MethodBuilder with(MethodElement e) {
        this.transform.accept(this.downstream, e);
        return this;
    }

    public ConstantPoolBuilder constantPool() {
        return this.downstream.constantPool();
    }

    public MethodBuilder withCode(Consumer<CodeBuilder> code) {
        this.downstream.withCode(code);
        return this;
    }

    public MethodBuilder transformCode(CodeModel code, CodeTransform xform) {
        this.downstream.transformCode(code, xform);
        return this;
    }
}

final class DroppingMethod implements MethodTransform {

    private final Predicate<MethodElement> filter;

    DroppingMethod(Predicate<MethodElement> filter) {
        this.filter = filter;
    }

    public void accept(MethodBuilder builder, MethodElement element) {
        if (!this.filter.test(element)) {
            builder.with(element);
        }
    }
}

final class EndHandlerMethod implements MethodTransform {

    private final Consumer<MethodBuilder> finisher;

    EndHandlerMethod(Consumer<MethodBuilder> finisher) {
        this.finisher = finisher;
    }

    public void accept(MethodBuilder builder, MethodElement element) {
        builder.with(element);
    }

    public void atEnd(MethodBuilder builder) {
        this.finisher.accept(builder);
    }
}

final class StatefulMethod implements MethodTransform {

    private final Supplier<MethodTransform> supplier;
    private MethodTransform current;

    StatefulMethod(Supplier<MethodTransform> supplier) {
        this.supplier = supplier;
    }

    public void atStart(MethodBuilder builder) {
        this.current = this.supplier.get();
        this.current.atStart(builder);
    }

    public void accept(MethodBuilder builder, MethodElement element) {
        this.current.accept(builder, element);
    }

    public void atEnd(MethodBuilder builder) {
        this.current.atEnd(builder);
        this.current = null;
    }
}

final class TransformingCode implements MethodTransform {

    private final CodeTransform xform;

    TransformingCode(CodeTransform xform) {
        this.xform = xform;
    }

    public void accept(MethodBuilder builder, MethodElement element) {
        if (element instanceof CodeModel) {
            builder.transformCode((CodeModel) element, this.xform);
        } else {
            builder.with(element);
        }
    }
}

// ---- campo ---------------------------------------------------------------------------------------

final class ChainedField implements FieldTransform {

    private final FieldTransform first;
    private final FieldTransform second;

    ChainedField(FieldTransform first, FieldTransform second) {
        this.first = first;
        this.second = second;
    }

    public void accept(FieldBuilder builder, FieldElement element) {
        this.first.accept(new ChainedFieldBuilder(builder, this.second), element);
    }

    public void atStart(FieldBuilder builder) {
        this.first.atStart(new ChainedFieldBuilder(builder, this.second));
        this.second.atStart(builder);
    }

    public void atEnd(FieldBuilder builder) {
        this.first.atEnd(new ChainedFieldBuilder(builder, this.second));
        this.second.atEnd(builder);
    }
}

final class ChainedFieldBuilder implements FieldBuilder {

    private final FieldBuilder downstream;
    private final FieldTransform transform;

    ChainedFieldBuilder(FieldBuilder downstream, FieldTransform transform) {
        this.downstream = downstream;
        this.transform = transform;
    }

    public FieldBuilder with(FieldElement e) {
        this.transform.accept(this.downstream, e);
        return this;
    }

    public ConstantPoolBuilder constantPool() {
        return this.downstream.constantPool();
    }
}

final class DroppingField implements FieldTransform {

    private final Predicate<FieldElement> filter;

    DroppingField(Predicate<FieldElement> filter) {
        this.filter = filter;
    }

    public void accept(FieldBuilder builder, FieldElement element) {
        if (!this.filter.test(element)) {
            builder.with(element);
        }
    }
}

final class EndHandlerField implements FieldTransform {

    private final Consumer<FieldBuilder> finisher;

    EndHandlerField(Consumer<FieldBuilder> finisher) {
        this.finisher = finisher;
    }

    public void accept(FieldBuilder builder, FieldElement element) {
        builder.with(element);
    }

    public void atEnd(FieldBuilder builder) {
        this.finisher.accept(builder);
    }
}

final class StatefulField implements FieldTransform {

    private final Supplier<FieldTransform> supplier;
    private FieldTransform current;

    StatefulField(Supplier<FieldTransform> supplier) {
        this.supplier = supplier;
    }

    public void atStart(FieldBuilder builder) {
        this.current = this.supplier.get();
        this.current.atStart(builder);
    }

    public void accept(FieldBuilder builder, FieldElement element) {
        this.current.accept(builder, element);
    }

    public void atEnd(FieldBuilder builder) {
        this.current.atEnd(builder);
        this.current = null;
    }
}

// ---- código --------------------------------------------------------------------------------------

final class ChainedCode implements CodeTransform {

    private final CodeTransform first;
    private final CodeTransform second;

    ChainedCode(CodeTransform first, CodeTransform second) {
        this.first = first;
        this.second = second;
    }

    public void accept(CodeBuilder builder, CodeElement element) {
        this.first.accept(new ChainedCodeBuilder(builder, this.second), element);
    }

    public void atStart(CodeBuilder builder) {
        this.first.atStart(new ChainedCodeBuilder(builder, this.second));
        this.second.atStart(builder);
    }

    public void atEnd(CodeBuilder builder) {
        this.first.atEnd(new ChainedCodeBuilder(builder, this.second));
        this.second.atEnd(builder);
    }
}

final class ChainedCodeBuilder implements CodeBuilder {

    private final CodeBuilder downstream;
    private final CodeTransform transform;

    ChainedCodeBuilder(CodeBuilder downstream, CodeTransform transform) {
        this.downstream = downstream;
        this.transform = transform;
    }

    public CodeBuilder with(CodeElement e) {
        this.transform.accept(this.downstream, e);
        return this;
    }

    public ConstantPoolBuilder constantPool() {
        return this.downstream.constantPool();
    }

    // Las etiquetas y los slots son del constructor de destino: una etiqueta pedida acá tiene que
    // resolver en el método que de verdad se está escribiendo, no en este intermediario.
    public Label newLabel() {
        return this.downstream.newLabel();
    }

    public Label startLabel() {
        return this.downstream.startLabel();
    }

    public Label endLabel() {
        return this.downstream.endLabel();
    }

    public int receiverSlot() {
        return this.downstream.receiverSlot();
    }

    public int parameterSlot(int paramNo) {
        return this.downstream.parameterSlot(paramNo);
    }

    public int allocateLocal(TypeKind typeKind) {
        return this.downstream.allocateLocal(typeKind);
    }

    public CodeBuilder.CatchBuilder catchBuilder(Label tryStart, Label tryEnd, Label end) {
        return this.downstream.catchBuilder(tryStart, tryEnd, end);
    }

    public CodeBuilder transformingBuilder(CodeTransform xform) {
        return new ChainedCodeBuilder(this, xform);
    }
}

final class EndHandlerCode implements CodeTransform {

    private final Consumer<CodeBuilder> finisher;

    EndHandlerCode(Consumer<CodeBuilder> finisher) {
        this.finisher = finisher;
    }

    public void accept(CodeBuilder builder, CodeElement element) {
        builder.with(element);
    }

    public void atEnd(CodeBuilder builder) {
        this.finisher.accept(builder);
    }
}

final class StatefulCode implements CodeTransform {

    private final Supplier<CodeTransform> supplier;
    private CodeTransform current;

    StatefulCode(Supplier<CodeTransform> supplier) {
        this.supplier = supplier;
    }

    public void atStart(CodeBuilder builder) {
        this.current = this.supplier.get();
        this.current.atStart(builder);
    }

    public void accept(CodeBuilder builder, CodeElement element) {
        this.current.accept(builder, element);
    }

    public void atEnd(CodeBuilder builder) {
        this.current.atEnd(builder);
        this.current = null;
    }
}
