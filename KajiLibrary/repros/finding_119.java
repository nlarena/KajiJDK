// Finding #119 — a type from a SUBPACKAGE of java.lang erases to java.lang.Object in the
// descriptor of a call site in another compilation unit.
//
// Our own java/lang/ref/WeakReference.class declares:
//     public WeakReference(Object, ReferenceQueue);
//       descriptor: (Ljava/lang/Object;Ljava/lang/ref/ReferenceQueue;)V
// but a caller compiled against it emits:
//     invokespecial java/lang/ref/WeakReference."<init>":(Ljava/lang/Object;Ljava/lang/Object;)V
//                                                                          ^^^^^^^^^^^^^^^^^^
// which is a guaranteed NoSuchMethodError at run time. The same happens for parameters and
// for return types (`ReferenceQueue poll()` becomes `()Ljava/lang/Object;`).
//
// It is specific to subpackages of java.lang: an identical hierarchy placed in `zz.ref`, or in
// `a.b.c`, or in a single-segment package, all compile correctly. The likely cause is the
// simple-name lookup falling back to the hard-coded JAVA_LANG path (looking for
// `java.lang.ReferenceQueue`), failing, and stubbing the unresolved type as Object instead of
// reporting an error — the same "resolution failure becomes silence" root as #108/#111/#118.
//
// LIVE IMPACT: KajiLibrary's WeakHashMap compiles and gates clean but cannot run — every
// WeakReference construction and every ReferenceQueue.poll() links against a descriptor that
// does not exist. There is no source-level workaround; the source is correct Java.
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class finding_119 {

    // BROKEN: the ReferenceQueue parameter erases to Object in the emitted descriptor.
    public WeakReference make(Object key, ReferenceQueue queue) {
        return new WeakReference(key, queue);
    }

    // BROKEN the same way in return position.
    public Object drain(ReferenceQueue queue) {
        return queue.poll();
    }
}
