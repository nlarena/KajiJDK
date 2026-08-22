package java.lang;

// KajiLibrary's java.lang.InheritableThreadLocal — a ThreadLocal whose value is meant to be
// handed down to threads a thread creates.
//
// THE PROBLEM IT ANSWERS. A plain ThreadLocal is per-thread and nothing more: a child thread
// starts with the initial value, not the parent's. That is usually right, but it breaks
// *ambient context* — a request id, a tenant, a security principal, a trace span — which is
// conceptually a property of the work being done, and the work continues in whatever threads it
// spawns. InheritableThreadLocal says: at the moment a Thread object is CONSTRUCTED, copy the
// creating thread's value into it.
//
// "At construction" is the part worth remembering. The snapshot is taken when the child is
// created, not when it starts and not continuously. A later `set()` in the parent is invisible
// to an already-created child, and a child's own `set()` never affects the parent. And it does
// not survive a thread pool: a pooled worker was constructed once, long before your task
// arrived, so it inherited from whoever created the pool.
//
// childValue() is the hook: it receives the parent's value and returns what the child should
// get. The default hands the SAME OBJECT over, which means parent and child now share one
// mutable object across threads — the classic trap with this class. Override it to copy.
//
// KAJILIBRARY STATUS — READ THIS BEFORE RELYING ON IT. The inheritance itself is not wired up.
// The copy has to happen inside Thread's constructor, which is where the JDK does it (it walks
// the parent's ThreadLocalMap and calls childValue on each inheritable entry), and KajiLibrary's
// java.lang.Thread does not yet carry that hook. So today this class behaves exactly like its
// superclass: correct as a ThreadLocal, with childValue() present, documented and callable, but
// never invoked by the runtime. Values do NOT currently propagate to child threads. The class
// is here so that the API and the semantics are in place for when Thread grows the hook; until
// then, pass context explicitly to a child thread rather than assuming it arrives.
public class InheritableThreadLocal<T> extends ThreadLocal<T> {

    public InheritableThreadLocal() {
    }

    // What a newly created child thread should see, given the creating thread's value. The
    // default is the parent's value itself — shared, not copied — so an override is required
    // whenever the value is mutable and the two threads must not stomp on each other.
    //
    // No calls are made on `parentValue` here: it is typed by a type variable, and finding #111
    // miscompiles a call on such a receiver into silence. Any override must bind it to an
    // Object (or a concrete type) before invoking anything on it.
    protected T childValue(T parentValue) {
        return parentValue;
    }
}
