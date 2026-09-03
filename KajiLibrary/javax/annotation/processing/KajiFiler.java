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
// QUE SOPORTA Y QUE NO — de las cuatro operaciones del contrato solo `createSourceFile` esta
// implementada, porque es la unica que el round loop de la VM sabe recibir: lo que se registra por
// el puente nativo se drena, se parsea, se compila y se reincorpora. Las otras tres no tienen a
// donde ir: `createClassFile` pediria emitir bytecode que nadie recogeria, y
// `createResource`/`getResource` pediria un `JavaFileManager` con locations reales, que este
// compilador no expone. Tiran `UnsupportedOperationException` (no-comprobada, igual que los flujos
// de bytes de `KajiSourceFile`) en vez de devolver un objeto que no escribe en ningun lado: fallar
// fuerte es honesto, devolver un `FileObject` mudo seria mentir.
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
                "KajiFiler solo genera fuentes: el round loop no recoge .class generados");
    }

    public FileObject createResource(JavaFileManager.Location location, CharSequence moduleAndPkg,
            CharSequence relativeName, Element... originatingElements) {
        throw new UnsupportedOperationException(
                "KajiFiler no tiene un JavaFileManager con locations donde crear recursos");
    }

    public FileObject getResource(JavaFileManager.Location location, CharSequence moduleAndPkg,
            CharSequence relativeName) {
        throw new UnsupportedOperationException(
                "KajiFiler no tiene un JavaFileManager con locations de donde leer recursos");
    }

    // Records this generated file with the VM: the interpreter pushes (name, heap offset of the
    // writer) onto a thread-local side channel that the compiler drains after the processor runs.
    // Non-private on purpose — the call site emits `invokevirtual`, which the native bridge
    // dispatches (a `private` call would go through `invokespecial`, which does not).
    native void nativeRegisterSourceFile(String name, StringWriter writer);
}
