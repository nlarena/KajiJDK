package javax.annotation.processing;

import javax.tools.JavaFileObject;
import java.io.StringWriter;

// KajiLibrary's Filer (APT fase 4) — the object a processor uses to create the source files it
// generates. createSourceFile(name) allocates a StringWriter to receive the generated text, hands
// the (name, writer) pair to the VM through the native bridge so the round loop can recover it, and
// returns a KajiSourceFile wrapping the same writer for the processor to write into.
public class KajiFiler implements Filer {

    // Note: the interface declares `throws IOException`; we narrow to nothing (allowed, §8.4.8.3),
    // which also sidesteps our javac reading external interfaces as declaring no checked throws.
    public JavaFileObject createSourceFile(CharSequence name) {
        String n = name.toString();
        StringWriter writer = new StringWriter();
        this.nativeRegisterSourceFile(n, writer);
        return new KajiSourceFile(n, writer);
    }

    // Records this generated file with the VM: the interpreter pushes (name, heap offset of the
    // writer) onto a thread-local side channel that the compiler drains after the processor runs.
    // Non-private on purpose — the call site emits `invokevirtual`, which the native bridge
    // dispatches (a `private` call would go through `invokespecial`, which does not).
    native void nativeRegisterSourceFile(String name, StringWriter writer);
}
