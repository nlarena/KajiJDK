package javax.annotation.processing;

import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import java.io.StringWriter;

// KajiLibrary's Filer (APT fase 4) — the object a processor uses to create the source files it
// generates. createSourceFile(name) allocates a StringWriter to receive the generated text, hands
// the (name, writer) pair to the VM through the native bridge so the round loop can recover it, and
// returns a KajiSourceFile wrapping the same writer for the processor to write into.
//
// WHAT IT SUPPORTS AND WHAT IT DOES NOT — of the contract's four operations only `createSourceFile`
// is implemented, because it is the only one the VM's round loop knows how to receive: what is
// registered through the native bridge is drained, parsed, compiled and fed back in. The other three
// have nowhere to go: `createClassFile` would ask for bytecode nobody would pick up, and
// `createResource`/`getResource` would ask for a `JavaFileManager` with real locations, which this
// compiler does not expose. They throw `UnsupportedOperationException` (unchecked, like
// `KajiSourceFile`'s byte streams) instead of returning an object that writes nowhere: failing loudly
// is honest, returning a mute `FileObject` would be lying.
public class KajiFiler implements Filer {

    // Note: the interface declares `throws IOException`; we narrow to nothing (allowed, §8.4.8.3),
    // which also sidesteps our javac reading external interfaces as declaring no checked throws.
    public JavaFileObject createSourceFile(CharSequence name, Element... originatingElements) {
        String n = name.toString();
        StringWriter writer = new StringWriter();
        this.nativeRegisterSourceFile(n, writer);
        return new KajiSourceFile(n, writer);
    }

    public JavaFileObject createClassFile(CharSequence name, Element... originatingElements) {
        throw new UnsupportedOperationException(
                "KajiFiler only generates sources: the round loop does not pick up generated .class");
    }

    public FileObject createResource(JavaFileManager.Location location, CharSequence moduleAndPkg,
            CharSequence relativeName, Element... originatingElements) {
        throw new UnsupportedOperationException(
                "KajiFiler has no JavaFileManager with locations to create resources in");
    }

    public FileObject getResource(JavaFileManager.Location location, CharSequence moduleAndPkg,
            CharSequence relativeName) {
        throw new UnsupportedOperationException(
                "KajiFiler has no JavaFileManager with locations to read resources from");
    }

    // Records this generated file with the VM: the interpreter pushes (name, heap offset of the
    // writer) onto a thread-local side channel that the compiler drains after the processor runs.
    // Non-private on purpose — the call site emits `invokevirtual`, which the native bridge
    // dispatches (a `private` call would go through `invokespecial`, which does not).
    native void nativeRegisterSourceFile(String name, StringWriter writer);
}
