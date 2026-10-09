package javax.annotation.processing;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.util.Set;

// The contract every annotation processor implements (JSR 269 §Processor).
//
// The **protocol** is the thing to understand, and it is rigid on purpose: the tool builds the
// processor with its no-argument constructor, asks it what it supports (the three `getSupported*`),
// gives it `init(env)` **exactly once**, and only then calls `process(...)` once per round until
// nothing more is generated, plus a final round with `processingOver() == true`. Never the other way
// round: asking before `init` is allowed, generating after the final round is not.
//
// In this project the one running that protocol is the VM itself (`src/jvm/interpreter/apt.rs`); the
// normal thing is not to implement this interface by hand but to extend `AbstractProcessor`.
public interface Processor {

    /** The `-A` options this processor understands. */
    Set<String> getSupportedOptions();

    /**
     * The annotation types this processor wants to see, by full name. `"*"` means all of them.
     */
    Set<String> getSupportedAnnotationTypes();

    /** The latest version of the language this processor understands. */
    SourceVersion getSupportedSourceVersion();

    /**
     * Hands it the environment. The tool calls it exactly once, before any `process`.
     */
    void init(ProcessingEnvironment processingEnv);

    /**
     * Processes one round.
     *
     * @return `true` if this processor **claims** those annotations, in which case they are not
     *         offered to any later processor. Returning `true` too eagerly is the classic way of
     *         breaking someone else's processor without noticing.
     */
    boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv);

    /**
     * Completion suggestions for the value of an annotation element, for an IDE. Returning an empty
     * collection is a valid answer and it is what almost every processor does.
     */
    Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation,
            ExecutableElement member, String userText);
}
