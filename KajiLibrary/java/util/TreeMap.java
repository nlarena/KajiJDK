package java.util;

// A map kept in **sorted** key order by a red-black tree — where {@link HashMap} scatters
// keys by hash and can only answer "is this key here?", this one keeps them ordered, so it
// also answers "what is the smallest key?" and could walk a range. The cost is O(log n) per
// operation instead of amortized O(1).
//
// A red-black tree is a binary search tree that stays balanced by colouring each node red or
// black and maintaining two invariants: **no red node has a red child**, and **every path
// from a node down to a leaf passes the same number of black nodes**. Together those bound
// the longest path at twice the shortest, which is what keeps the depth logarithmic. Every
// insertion and deletion restores them with a handful of recolourings and rotations — a
// rotation being the one move that changes the shape without breaking the search order.
//
// Ordering comes from the keys' own {@link Comparable}, or from a {@link Comparator} handed
// to the constructor.
//
// Subset of the JDK's: the NavigableMap/SortedMap navigation (headMap/tailMap/ceilingKey/…)
// and the collection views (keySet/values/entrySet) are not modelled — our `Map` has neither.
public class TreeMap<K, V> implements Map<K, V> {

    private TmNode<K, V> root;
    private int size;
    // Null means "use the keys' natural ordering".
    private final Comparator<K> comparator;

    public TreeMap() {
        this.comparator = null;
    }

    public TreeMap(Comparator<K> comparator) {
        this.comparator = comparator;
    }

    public Comparator<K> comparator() {
        return comparator;
    }

    // Compare two keys, by the comparator if there is one and by natural ordering otherwise.
    // Both arguments are taken as `Object` on purpose: calling a method on a receiver whose
    // static type is a *type variable* is silently dropped by our javac (finding #111), so
    // the natural-ordering path binds the key to a `Comparable` local first.
    private int compare(Object a, Object b) {
        int c;
        if (comparator != null) {
            c = comparator.compare((K) a, (K) b);
        } else {
            Comparable<Object> ca = (Comparable<Object>) a;
            c = ca.compareTo(b);
        }
        return c;
    }

    // The node holding `key`, or null.
    private TmNode<K, V> getNode(Object key) {
        TmNode<K, V> p = root;
        TmNode<K, V> found = null;
        while (p != null && found == null) {
            int c = compare(key, p.key);
            if (c < 0) {
                p = p.left;
            } else if (c > 0) {
                p = p.right;
            } else {
                found = p;
            }
        }
        return found;
    }

    // --- Map ---

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(Object key) {
        return getNode(key) != null;
    }

    public V get(Object key) {
        TmNode<K, V> p = getNode(key);
        V v;
        if (p == null) {
            v = null;
        } else {
            v = p.value;
        }
        return v;
    }

    public boolean containsValue(Object value) {
        boolean found = false;
        TmNode<K, V> p = firstNode();
        while (p != null) {
            // `p.value` is a type variable, so it is read into an Object before comparing
            // (finding #111 again).
            Object v = p.value;
            if (value == null) {
                if (v == null) {
                    found = true;
                }
            } else if (value.equals(v)) {
                found = true;
            }
            p = successor(p);
        }
        return found;
    }

    public V put(K key, V value) {
        V old = null;
        if (root == null) {
            root = new TmNode<K, V>(key, value, null);
            root.red = false;
            size = 1;
        } else {
            // Descend to the insertion point, remembering the parent.
            TmNode<K, V> p = root;
            TmNode<K, V> parent = null;
            int c = 0;
            boolean replaced = false;
            while (p != null && !replaced) {
                parent = p;
                c = compare(key, p.key);
                if (c < 0) {
                    p = p.left;
                } else if (c > 0) {
                    p = p.right;
                } else {
                    old = p.value;
                    p.value = value;
                    replaced = true;
                }
            }
            if (!replaced) {
                TmNode<K, V> node = new TmNode<K, V>(key, value, parent);
                if (c < 0) {
                    parent.left = node;
                } else {
                    parent.right = node;
                }
                size++;
                fixAfterInsertion(node);
            }
        }
        return old;
    }

    public V remove(Object key) {
        TmNode<K, V> p = getNode(key);
        V old;
        if (p == null) {
            old = null;
        } else {
            old = p.value;
            deleteNode(p);
        }
        return old;
    }

    public void clear() {
        root = null;
        size = 0;
    }

    // --- ordered access ---

    public K firstKey() {
        TmNode<K, V> p = firstNode();
        if (p == null) {
            throw new NoSuchElementException();
        }
        return p.key;
    }

    public K lastKey() {
        TmNode<K, V> p = lastNode();
        if (p == null) {
            throw new NoSuchElementException();
        }
        return p.key;
    }

    // Package-private from here down: TreeSet walks the tree through these to implement its
    // iterators, since our `Map` has no keySet view to borrow one from.
    TmNode<K, V> firstNode() {
        TmNode<K, V> p = root;
        if (p != null) {
            while (p.left != null) {
                p = p.left;
            }
        }
        return p;
    }

    TmNode<K, V> lastNode() {
        TmNode<K, V> p = root;
        if (p != null) {
            while (p.right != null) {
                p = p.right;
            }
        }
        return p;
    }

    // The node with the next key in order: the leftmost of the right subtree, or —
    // failing that — the first ancestor this node is *not* in the right subtree of.
    TmNode<K, V> successor(TmNode<K, V> t) {
        TmNode<K, V> result;
        if (t == null) {
            result = null;
        } else if (t.right != null) {
            TmNode<K, V> p = t.right;
            while (p.left != null) {
                p = p.left;
            }
            result = p;
        } else {
            TmNode<K, V> p = t.parent;
            TmNode<K, V> ch = t;
            while (p != null && ch == p.right) {
                ch = p;
                p = p.parent;
            }
            result = p;
        }
        return result;
    }

    // The mirror of successor: the node with the previous key in order.
    TmNode<K, V> predecessor(TmNode<K, V> t) {
        TmNode<K, V> result;
        if (t == null) {
            result = null;
        } else if (t.left != null) {
            TmNode<K, V> p = t.left;
            while (p.right != null) {
                p = p.right;
            }
            result = p;
        } else {
            TmNode<K, V> p = t.parent;
            TmNode<K, V> ch = t;
            while (p != null && ch == p.left) {
                ch = p;
                p = p.parent;
            }
            result = p;
        }
        return result;
    }

    // --- red-black machinery ---
    //
    // The accessors below are null-tolerant on purpose: in the fix-up loops a nil child is
    // a legitimate BLACK leaf, so treating null as black (and as having no parent) removes
    // a swarm of null checks from the algorithms themselves.

    private boolean isRed(TmNode<K, V> p) {
        boolean red;
        if (p == null) {
            red = false;
        } else {
            red = p.red;
        }
        return red;
    }

    private void setColor(TmNode<K, V> p, boolean red) {
        if (p != null) {
            p.red = red;
        }
    }

    private TmNode<K, V> parentOf(TmNode<K, V> p) {
        TmNode<K, V> n;
        if (p == null) {
            n = null;
        } else {
            n = p.parent;
        }
        return n;
    }

    private TmNode<K, V> leftOf(TmNode<K, V> p) {
        TmNode<K, V> n;
        if (p == null) {
            n = null;
        } else {
            n = p.left;
        }
        return n;
    }

    private TmNode<K, V> rightOf(TmNode<K, V> p) {
        TmNode<K, V> n;
        if (p == null) {
            n = null;
        } else {
            n = p.right;
        }
        return n;
    }

    // Pivot `p` down-left: its right child takes its place. Search order is preserved
    // because everything between them stays between them.
    private void rotateLeft(TmNode<K, V> p) {
        if (p != null) {
            TmNode<K, V> r = p.right;
            p.right = r.left;
            if (r.left != null) {
                r.left.parent = p;
            }
            r.parent = p.parent;
            if (p.parent == null) {
                root = r;
            } else if (p.parent.left == p) {
                p.parent.left = r;
            } else {
                p.parent.right = r;
            }
            r.left = p;
            p.parent = r;
        }
    }

    private void rotateRight(TmNode<K, V> p) {
        if (p != null) {
            TmNode<K, V> l = p.left;
            p.left = l.right;
            if (l.right != null) {
                l.right.parent = p;
            }
            l.parent = p.parent;
            if (p.parent == null) {
                root = l;
            } else if (p.parent.right == p) {
                p.parent.right = l;
            } else {
                p.parent.left = l;
            }
            l.right = p;
            p.parent = l;
        }
    }

    // A freshly inserted node is red, which can only break the "no red-red" invariant.
    // Two cases: if the uncle is red, recolour parent/uncle black and grandparent red and
    // carry the problem two levels up; if it is black, one or two rotations settle it for
    // good.
    private void fixAfterInsertion(TmNode<K, V> x) {
        x.red = true;
        while (x != null && x != root && isRed(x.parent)) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                TmNode<K, V> y = rightOf(parentOf(parentOf(x)));
                if (isRed(y)) {
                    setColor(parentOf(x), false);
                    setColor(y, false);
                    setColor(parentOf(parentOf(x)), true);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), false);
                    setColor(parentOf(parentOf(x)), true);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                TmNode<K, V> y = leftOf(parentOf(parentOf(x)));
                if (isRed(y)) {
                    setColor(parentOf(x), false);
                    setColor(y, false);
                    setColor(parentOf(parentOf(x)), true);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), false);
                    setColor(parentOf(parentOf(x)), true);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.red = false;
    }

    // Deleting a node with two children is turned into deleting one with at most one, by
    // copying the successor's entry over it and removing the successor instead.
    private void deleteNode(TmNode<K, V> p) {
        size--;
        if (p.left != null && p.right != null) {
            TmNode<K, V> s = successor(p);
            p.key = s.key;
            p.value = s.value;
            p = s;
        }
        TmNode<K, V> replacement;
        if (p.left != null) {
            replacement = p.left;
        } else {
            replacement = p.right;
        }
        if (replacement != null) {
            replacement.parent = p.parent;
            if (p.parent == null) {
                root = replacement;
            } else if (p == p.parent.left) {
                p.parent.left = replacement;
            } else {
                p.parent.right = replacement;
            }
            p.left = null;
            p.right = null;
            p.parent = null;
            // Removing a black node shortens every path through it by one black.
            if (!p.red) {
                fixAfterDeletion(replacement);
            }
        } else if (p.parent == null) {
            root = null;
        } else {
            // A black leaf: rebalance *before* unlinking, while it still has a parent.
            if (!p.red) {
                fixAfterDeletion(p);
            }
            if (p.parent != null) {
                if (p == p.parent.left) {
                    p.parent.left = null;
                } else if (p == p.parent.right) {
                    p.parent.right = null;
                }
                p.parent = null;
            }
        }
    }

    // `x` carries one "extra black" that has to be discharged: either by borrowing a red
    // from a sibling (rotations), or by pushing the deficit up to the parent and repeating.
    private void fixAfterDeletion(TmNode<K, V> x) {
        while (x != root && !isRed(x)) {
            if (x == leftOf(parentOf(x))) {
                TmNode<K, V> sib = rightOf(parentOf(x));
                if (isRed(sib)) {
                    setColor(sib, false);
                    setColor(parentOf(x), true);
                    rotateLeft(parentOf(x));
                    sib = rightOf(parentOf(x));
                }
                if (!isRed(leftOf(sib)) && !isRed(rightOf(sib))) {
                    setColor(sib, true);
                    x = parentOf(x);
                } else {
                    if (!isRed(rightOf(sib))) {
                        setColor(leftOf(sib), false);
                        setColor(sib, true);
                        rotateRight(sib);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, isRed(parentOf(x)));
                    setColor(parentOf(x), false);
                    setColor(rightOf(sib), false);
                    rotateLeft(parentOf(x));
                    x = root;
                }
            } else {
                TmNode<K, V> sib = leftOf(parentOf(x));
                if (isRed(sib)) {
                    setColor(sib, false);
                    setColor(parentOf(x), true);
                    rotateRight(parentOf(x));
                    sib = leftOf(parentOf(x));
                }
                if (!isRed(rightOf(sib)) && !isRed(leftOf(sib))) {
                    setColor(sib, true);
                    x = parentOf(x);
                } else {
                    if (!isRed(leftOf(sib))) {
                        setColor(rightOf(sib), false);
                        setColor(sib, true);
                        rotateLeft(sib);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, isRed(parentOf(x)));
                    setColor(parentOf(x), false);
                    setColor(leftOf(sib), false);
                    rotateRight(parentOf(x));
                    x = root;
                }
            }
        }
        setColor(x, false);
    }

    // --- test seam ---

    // The tree's black-height as seen from the root, or -1 if the red-black invariants are
    // broken. Package-private: it exists so a test can assert the tree really is balanced,
    // which is the only way to tell a working red-black tree from a plain BST that happens
    // to answer the same queries.
    int checkInvariants() {
        int height;
        if (isRed(root)) {
            height = -1;                       // the root must be black
        } else {
            height = blackHeight(root);
        }
        return height;
    }

    private int blackHeight(TmNode<K, V> p) {
        int height;
        if (p == null) {
            height = 1;                        // a nil leaf counts as one black
        } else if (isRed(p) && (isRed(p.left) || isRed(p.right))) {
            height = -1;                       // red node with a red child
        } else {
            int left = blackHeight(p.left);
            int right = blackHeight(p.right);
            if (left < 0 || right < 0 || left != right) {
                height = -1;                   // unequal black paths
            } else if (isRed(p)) {
                height = left;
            } else {
                height = left + 1;
            }
        }
        return height;
    }
}

// One tree node: its entry, its three links and its colour. Top-level package-private
// rather than nested, since a nested class inside a *generic* class is miscompiled
// (finding #13).
final class TmNode<K, V> {

    K key;
    V value;
    TmNode<K, V> left;
    TmNode<K, V> right;
    TmNode<K, V> parent;
    // A new node always starts red; the fix-up decides what it ends up as.
    boolean red = true;

    TmNode(K key, V value, TmNode<K, V> parent) {
        this.key = key;
        this.value = value;
        this.parent = parent;
    }
}
