// A7 #6 (JLS §10.7): Object.clone() + Cloneable.
//
// CnPoint opts in (implements Cloneable): clone() yields a NEW object of the same
// class with the fields copied verbatim; mutating the clone must not touch the
// original. CnPlain does NOT opt in: clone() throws CloneNotSupportedException
// (checked — the caller catches it). Arrays implement Cloneable implicitly:
// int[].clone() copies length + elements into a distinct array.
//
// Score (no autoboxing anywhere):
//   original untouched after mutating the clone (3 + 4)      =  7
//   clone carried the copied values before mutation (3 + 4)  = 14
//   mutated clone reads back its own values (8 + 4 - 2)      = 24
//   clone is a distinct object (orig != copy)                = 29
//   CnPlain.clone() threw CloneNotSupportedException, caught = 31
//   CnVec's override delegating to super.clone() copied z    = 35
//   int[] clone: distinct, right length, elements copied,
//   original untouched by the clone's mutation               = 42
class CnPoint implements Cloneable {
    int x;
    int y;

    CnPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public CnPoint copy() {
        try {
            return (CnPoint) clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}

class CnPlain {
    // No Cloneable: the VM must refuse with CloneNotSupportedException.
    public Object copy() throws CloneNotSupportedException {
        return clone();
    }
}

class CnVec implements Cloneable {
    int z;

    // The canonical override shape: widen access and delegate to super.clone() —
    // which javac compiles to *invokespecial* java/lang/Object.clone (the
    // statically-bound path, distinct from CnPoint's invokevirtual one).
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

public class CnTest {
    static int run() {
        int score = 0;

        // --- opt-in class: shallow copy, then mutate only the clone ---------------
        CnPoint orig = new CnPoint(3, 4);
        CnPoint copy = orig.copy();
        int copiedX = copy.x; // snapshot the copied fields before mutating
        int copiedY = copy.y;
        copy.x = copy.x + 5;

        score += orig.x + orig.y;   // 7  — original untouched (3 + 4)
        score += copiedX + copiedY; // 14 — clone carried the values (3 + 4)
        score += copy.x;            // 22 — mutated clone (3 + 5 = 8)
        score += copy.y - 2;        // 24 — clone's y still 4
        if (orig != copy) {
            score += 5;             // 29 — a genuinely distinct object
        }

        // --- no opt-in: clone refuses ---------------------------------------------
        CnPlain plain = new CnPlain();
        try {
            plain.copy();
        } catch (CloneNotSupportedException e) {
            score += 2;             // 31
        }

        // --- an override delegating to super.clone() (invokespecial) ---------------
        CnVec v = new CnVec();
        v.z = 4;
        try {
            CnVec w = (CnVec) v.clone();
            if (w != v) {
                score += w.z;       // 35 — the copied field came through super.clone()
            }
        } catch (CloneNotSupportedException e) {
            score -= 100;           // must not happen: CnVec opted in
        }

        // --- arrays: implicitly Cloneable -----------------------------------------
        int[] nums = new int[3];
        nums[0] = 1;
        nums[1] = 2;
        nums[2] = 3;
        int[] dup = nums.clone();
        dup[1] = 40;
        if (dup != nums && dup.length == 3) {
            score += dup[0] + nums[1] + dup[2]; // 41 — 1 + 2 + 3 (original's [1] intact)
            score += dup[1] - 39;               // 42 — the clone's own element mutated
        }

        return score;
    }
}
