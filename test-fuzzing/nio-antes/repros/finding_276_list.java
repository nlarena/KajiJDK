/**
 * #276 (List half) — `List` is a `SequencedCollection`: working ends, and a `reversed()` that
 * returns a real VIEW.
 *
 *   bin/javac.exe --emit -cp KajiLibrary KajiLibrary/repros/finding_276_list.java
 *   bin/run-headless.exe KajiLibrary/repros/finding_276_list.class run
 *
 * One bit per property, so a partial failure names itself instead of collapsing into "broken":
 *
 *    1  reversed() reverses the reading order
 *    2  set(i, e) through the view writes into the BASE (it is a view, not a copy)
 *    4  a write to the base is visible through a view taken earlier (the other direction)
 *    8  reversed().reversed() is the base list itself, by identity
 *   16  the view's iterator() walks the base backwards
 *   32  getFirst/getLast on a list
 *   64  getFirst on an empty list throws NoSuchElementException (not UnsupportedOperation)
 *  128  addFirst/addLast put elements at the ends
 *  256  removeFirst/removeLast take them off and return them
 *  512  add(E) through the view appends to the view, i.e. PREPENDS to the base
 * 1024  add(int, E) through the view lands at that view index
 * 2048  remove(int) and remove(Object) through the view hit the base
 * 4096  size/isEmpty/contains/indexOf/clear through the view
 * 8192  the ends on the VIEW are the base's ends, swapped
 * 16384 dispatch through the SequencedCollection static type reaches the narrowed reversed()
 * 32768 LinkedList: it implements List AND Deque, which both spell the ends — Deque's concrete
 *       methods must win over List's defaults, and reversed() must still view it
 * 65536 the same view over Vector and CopyOnWriteArrayList
 *
 * Expected: 0. Anything else is the OR of the bits that failed.
 */
public class finding_276_list {

    private static java.util.List<String> abc() {
        java.util.List<String> l = new java.util.ArrayList<String>();
        l.add("a");
        l.add("b");
        l.add("c");
        return l;
    }

    public static int run() {
        int bad = 0;

        // 1 — reading order is inverted.
        java.util.List<String> base = abc();
        java.util.List<String> rev = base.reversed();
        if (rev.size() != 3 || !rev.get(0).equals("c") || !rev.get(1).equals("b")
                || !rev.get(2).equals("a")) {
            bad = bad | 1;
        }

        // 2 — writing through the view lands in the base.
        base = abc();
        rev = base.reversed();
        String old = rev.set(0, "Z");
        if (!"c".equals(old) || !base.get(2).equals("Z") || !rev.get(0).equals("Z")) {
            bad = bad | 2;
        }

        // 4 — and the view is live: a later write to the base shows up through it.
        base = abc();
        rev = base.reversed();
        base.set(0, "Y");
        if (!rev.get(2).equals("Y")) {
            bad = bad | 4;
        }

        // 8 — reversing twice gives back the very same object, not a third wrapper.
        base = abc();
        if (base.reversed().reversed() != base) {
            bad = bad | 8;
        }

        // 16 — the iterator walks backwards.
        base = abc();
        StringBuilder sb = new StringBuilder();
        java.util.Iterator<String> it = base.reversed().iterator();
        while (it.hasNext()) {
            sb.append(it.next());
        }
        if (!sb.toString().equals("cba")) {
            bad = bad | 16;
        }

        // 32 — the ends of a plain list.
        base = abc();
        if (!base.getFirst().equals("a") || !base.getLast().equals("c")) {
            bad = bad | 32;
        }

        // 64 — no ends on an empty list: NoSuchElementException, and it must not be caught as
        // UnsupportedOperationException (which is what SequencedCollection's default would throw).
        java.util.List<String> empty = new java.util.ArrayList<String>();
        int caught = 0;
        try {
            empty.getFirst();
        } catch (java.util.NoSuchElementException e) {
            caught = caught + 1;
        }
        try {
            empty.getLast();
        } catch (java.util.NoSuchElementException e) {
            caught = caught + 1;
        }
        try {
            empty.removeFirst();
        } catch (java.util.NoSuchElementException e) {
            caught = caught + 1;
        }
        try {
            empty.removeLast();
        } catch (java.util.NoSuchElementException e) {
            caught = caught + 1;
        }
        if (caught != 4) {
            bad = bad | 64;
        }

        // 128 — insertion at the ends.
        base = abc();
        base.addFirst("start");
        base.addLast("end");
        if (base.size() != 5 || !base.get(0).equals("start") || !base.get(4).equals("end")) {
            bad = bad | 128;
        }

        // 256 — removal at the ends returns what it took.
        base = abc();
        String head = base.removeFirst();
        String tail = base.removeLast();
        if (!"a".equals(head) || !"c".equals(tail) || base.size() != 1
                || !base.get(0).equals("b")) {
            bad = bad | 256;
        }

        // 512 — appending to the view prepends to the base.
        base = abc();
        rev = base.reversed();
        rev.add("new");
        if (base.size() != 4 || !base.get(0).equals("new") || !rev.get(3).equals("new")) {
            bad = bad | 512;
        }

        // 1024 — positional insertion is judged in VIEW coordinates.
        base = abc();
        rev = base.reversed();
        rev.add(1, "X");
        // view: c X b a  ->  base: a b X c
        if (!rev.get(1).equals("X") || !base.get(2).equals("X") || base.size() != 4) {
            bad = bad | 1024;
        }

        // 2048 — removal through the view, by index and by value.
        base = abc();
        rev = base.reversed();
        String gone = rev.remove(0);
        if (!"c".equals(gone) || base.size() != 2 || !base.get(1).equals("b")) {
            bad = bad | 2048;
        }
        base = abc();
        rev = base.reversed();
        if (!rev.remove("a") || base.size() != 2 || !base.get(0).equals("b")
                || rev.remove("nope")) {
            bad = bad | 2048;
        }

        // 4096 — the rest of Collection, through the view.
        base = abc();
        rev = base.reversed();
        if (rev.size() != 3 || rev.isEmpty() || !rev.contains("a") || rev.contains("q")
                || rev.indexOf("a") != 2 || rev.indexOf("c") != 0 || rev.indexOf("q") != -1) {
            bad = bad | 4096;
        }
        rev.clear();
        if (!rev.isEmpty() || !base.isEmpty()) {
            bad = bad | 4096;
        }

        // 8192 — the ends of the view are the base's, swapped.
        base = abc();
        rev = base.reversed();
        if (!rev.getFirst().equals("c") || !rev.getLast().equals("a")) {
            bad = bad | 8192;
        }
        rev.addFirst("z");
        if (!base.getLast().equals("z") || !rev.getFirst().equals("z")) {
            bad = bad | 8192;
        }

        // 16384 — the covariant default: seen as a SequencedCollection, reversed() must still
        // find List's override (this is the bridge method in the interface).
        base = abc();
        java.util.SequencedCollection<String> sc = base;
        java.util.SequencedCollection<String> scRev = sc.reversed();
        if (scRev == null || scRev.size() != 3 || !scRev.getFirst().equals("c")) {
            bad = bad | 16384;
        }

        // 32768 — LinkedList sits under both List and Deque. Deque declares the ends abstract, so
        // its concrete implementations must be what runs; and the view has to work over links too
        // (this is the O(n^2) iteration the view documents).
        java.util.LinkedList<String> ll = new java.util.LinkedList<String>();
        ll.add("a");
        ll.add("b");
        ll.add("c");
        if (!ll.getFirst().equals("a") || !ll.getLast().equals("c")) {
            bad = bad | 32768;
        }
        java.util.List<String> llRev = ll.reversed();
        StringBuilder lsb = new StringBuilder();
        java.util.Iterator<String> lit = llRev.iterator();
        while (lit.hasNext()) {
            lsb.append(lit.next());
        }
        if (!lsb.toString().equals("cba")) {
            bad = bad | 32768;
        }
        llRev.set(0, "W");
        if (!ll.getLast().equals("W")) {
            bad = bad | 32768;
        }

        // 65536 — and over the other two indexed lists.
        java.util.Vector<String> v = new java.util.Vector<String>();
        v.add("a");
        v.add("b");
        java.util.List<String> vRev = v.reversed();
        if (!vRev.get(0).equals("b") || !v.getFirst().equals("a") || !v.getLast().equals("b")) {
            bad = bad | 65536;
        }
        java.util.concurrent.CopyOnWriteArrayList<String> cw =
                new java.util.concurrent.CopyOnWriteArrayList<String>();
        cw.add("a");
        cw.add("b");
        java.util.List<String> cwRev = cw.reversed();
        if (!cwRev.get(0).equals("b") || !cw.getFirst().equals("a") || !cw.getLast().equals("b")) {
            bad = bad | 65536;
        }

        return bad;
    }

    public static void main(String[] args) {
        System.out.println("run " + finding_276_list.run());
    }
}
