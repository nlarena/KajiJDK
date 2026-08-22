// Finding #108 — a chained call whose INTERMEDIATE returns an interface type is silently
// DROPPED, and a stray `pop` is emitted in its place (corrupting the operand stack).
//
//   iface().act();     // emitted: just `pop`  — the two calls vanish
//   Iface v = iface(); v.act();   // CORRECT: invokevirtual + invokeinterface
//
// Chaining through a *class*-typed intermediate compiles correctly, so the trigger is the
// interface-typed receiver of the second call. Found building ReentrantReadWriteLock:
// `lock.writeLock().lock()` compiled to nothing, so every lock/unlock silently vanished.
public class finding_108 {
    interface Iface {
        void act();
    }
    static class Impl implements Iface {
        public void act() {}
    }
    private final Iface impl = new Impl();
    Iface iface() { return impl; }
    Impl clazz() { return (Impl) impl; }

    void demo() {
        iface().act();   // BROKEN: statement dropped, stray `pop`
        clazz().act();   // OK: class-typed intermediate chains correctly
        Iface v = iface();
        v.act();         // OK: workaround — bind to a local, then call
    }
}
