package javax.annotation.processing;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// The base class nearly every real processor inherits from (JSR 269 §AbstractProcessor). The only
// thing it leaves abstract is `process`; everything else it resolves by reading the subclass's
// `@SupportedAnnotationTypes`, `@SupportedOptions` and `@SupportedSourceVersion` by reflection.
//
// ============================================================================================
//  IMPORTANT WARNING — the three `getSupported*` return their default value in THIS VM
// ============================================================================================
//
// This class's mechanism is to read its own annotations at run time. In KajiJDK that **does not work
// yet**, and not because of how this class is written but because of a bug in the project's
// compiler:
//
//   our javac does not emit the `RuntimeVisibleAnnotations` attribute when the type of the applied
//   annotation resolves from the **classpath** (a `.class`), instead of being declared in the same
//   compilation unit.
//
// A user's processor is always in that case: `@SupportedAnnotationTypes` lives in KajiLibrary, that
// is, on the classpath. So its `.class` comes out without the annotation, and
// `getClass().getAnnotation(SupportedAnnotationTypes.class)` returns `null` — measured, not assumed.
// (The same bug explains why the library's own `SupportedAnnotationTypes.class` lost its
// `@Retention(RUNTIME)`: it was compiled reading `@Retention` from the classpath.)
//
// The concrete consequence: `getSupportedAnnotationTypes()` returns the empty set,
// `getSupportedOptions()` too, and `getSupportedSourceVersion()` returns `RELEASE_6`, for **any**
// processor, whether or not it has the annotations on it.
//
// It was written with the real logic all the same, and on purpose: the code is correct — it does
// exactly what the contract demands with the input it receives — and it will start giving the right
// answer on its own as soon as the compiler emits the annotations. Faking the result (returning
// `{"*"}`, say) would be lying about what the processor declared.
//
// That this breaks nothing today has a specific reason: this project's round loop
// (`src/jvm/interpreter/apt.rs`) consults none of the three — it builds the processor, gives it
// `init(env)` and calls `process(...)` each round. Filtering by annotation type does not exist yet,
// so a processor gets every round regardless.
//
// ONE DELIBERATE OMISSION, for the same reason. The real JDK, when it does not find the annotation
// and the processor is already initialized, warns through the `Messager`: "No SupportedSourceVersion
// annotation found on X, returning RELEASE_6". Here that warning is **not** emitted, and on purpose:
// since the annotation is never seen, the warning would come out for every processor — including the
// ones that DO have it. It would be telling someone they forgot something they actually wrote. A
// warning that lies is worse than no warning; the honest explanation is this header.
public abstract class AbstractProcessor implements Processor {

    /** The environment `init` left. `protected` because the subclasses use it directly. */
    protected ProcessingEnvironment processingEnv;

    // Whether it has been through `init`. It is read and written under the instance's lock (see
    // `init` and `isInitialized`, both `synchronized`): the tool may initialize on one thread and
    // process on another.
    private boolean initialized = false;

    /** For the subclasses only. */
    protected AbstractProcessor() {
    }

    /**
     * The options from `@SupportedOptions`, or the empty set if it is not there.
     *
     * <p>See the header's warning: today it is always the empty set.
     */
    public Set<String> getSupportedOptions() {
        SupportedOptions so = this.getClass().getAnnotation(SupportedOptions.class);
        if (so == null) {
            return Collections.emptySet();
        }
        return arrayToSet(so.value());
    }

    /**
     * The types from `@SupportedAnnotationTypes`, or the empty set if it is not there.
     *
     * <p>See the header's warning: today it is always the empty set.
     */
    public Set<String> getSupportedAnnotationTypes() {
        SupportedAnnotationTypes sat = this.getClass().getAnnotation(SupportedAnnotationTypes.class);
        if (sat == null) {
            return Collections.emptySet();
        }
        return arrayToSet(sat.value());
    }

    /**
     * The version from `@SupportedSourceVersion`, or `RELEASE_6` if it is not there.
     *
     * <p>`RELEASE_6` and not `latest()`: it is the default the contract fixes, and it is deliberately
     * low so that a processor which says nothing does not promise to understand constructs it does
     * not know. See the header's warning: today it always falls back to this default.
     */
    public SourceVersion getSupportedSourceVersion() {
        SupportedSourceVersion ssv = this.getClass().getAnnotation(SupportedSourceVersion.class);
        if (ssv == null) {
            return SourceVersion.RELEASE_6;
        }
        return ssv.value();
    }

    /**
     * Stores the environment. `synchronized`, and it refuses the second call: the contract says
     * "exactly once", and a processor reinitialized halfway would be left with a `Filer` from another
     * run.
     *
     * @throws IllegalStateException if it has already been called
     */
    public synchronized void init(ProcessingEnvironment processingEnv) {
        if (this.initialized) {
            throw new IllegalStateException("Cannot call init more than once.");
        }
        Objects.requireNonNull(processingEnv, "Tool provided null ProcessingEnvironment");
        this.processingEnv = processingEnv;
        this.initialized = true;
    }

    /** The only thing the subclass has to write. */
    public abstract boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv);

    /**
     * No suggestions. It is the right answer for a processor that offers no completion, and the one
     * the real JDK gives from this base class.
     */
    public Iterable<? extends Completion> getCompletions(Element element,
            AnnotationMirror annotation, ExecutableElement member, String userText) {
        return NO_COMPLETIONS;
    }

    // It is kept in a field with the type written out instead of returning `Collections.emptyList()`
    // in the `return`: our javac does not infer `T` when the target is a supertype with a wildcard
    // (`List<T>` against `Iterable<? extends Completion>`) and rejects the call. With the type fixed
    // here no inference is needed — and the empty list is created exactly once.
    private static final List<Completion> NO_COMPLETIONS =
            Collections.unmodifiableList(new ArrayList<Completion>());

    /** Whether it has been through {@link #init}. */
    protected synchronized boolean isInitialized() {
        return this.initialized;
    }

    // Internal: copies an annotation's array into an immutable set. `LinkedHashSet` so as not to
    // lose the order they were declared in (the contract does not require it, but a stable order
    // keeps the diagnostic messages from dancing about), and a **copy** because `value()` returns the
    // annotation's cloned array and we do not want the set tied to it.
    private static Set<String> arrayToSet(String[] array) {
        Set<String> set = new LinkedHashSet<String>();
        for (String s : array) {
            set.add(s);
        }
        return Collections.unmodifiableSet(set);
    }
}
