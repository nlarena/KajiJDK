package java.lang.ref;

// The queue references are enqueued onto once their referent dies. A singly-linked
// stack threaded through Reference.next; the GC pushes, the program polls.
public class ReferenceQueue {
    Reference head;   // top of the pending list (offset 8); GC links new ones here

    public Reference poll() {
        if (head == null) {
            return null;
        }
        Reference r = head;
        head = r.next;
        r.next = null;
        return r;
    }
}
