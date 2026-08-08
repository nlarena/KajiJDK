package java.lang.ref;

import java.lang.ref.Reference; // redundante (mismo paquete); workaround del class-finder, ver WeakReference

// KajiLibrary's java.lang.ref.ReferenceQueue — the queue References are enqueued onto once
// their referent dies. A singly-linked stack threaded through Reference.next: the GC
// pushes cleared references, the program polls them off.
public class ReferenceQueue {

    Reference head;   // top of the pending list; the GC links newly-cleared references here

    // Removes and returns the next pending reference, or null if the queue is empty.
    public Reference poll() {
        if (this.head == null) {
            return null;
        }
        Reference r = this.head;
        this.head = r.next;
        r.next = null;
        return r;
    }
}
