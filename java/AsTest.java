// A7 item 3: ArrayStoreException (JVMS §6.5 `aastore`). Arrays are covariant, so an
// AsDog[] can hide behind an AsAnimal[] variable — the VM must check every reference
// store dynamically. A valid store (AsDog into a Dog[] slot) succeeds (+10), an
// invalid one (AsCat into the same Dog[]) throws ArrayStoreException (+20), and a
// null store is always legal (+12). Deterministic score → green ≡ os-gil ≡ os = 42.
class AsAnimal {
}

class AsDog extends AsAnimal {
}

class AsCat extends AsAnimal {
}

public class AsTest {
    static int run() {
        int score = 0;
        AsAnimal[] arr = new AsDog[2]; // covariance: Dog[] seen as Animal[]
        arr[0] = new AsDog(); // runtime class AsDog is assignable to AsDog → OK
        if (arr[0] != null) {
            score += 10;
        }
        try {
            arr[1] = new AsCat(); // AsCat is NOT an AsDog → ArrayStoreException
        } catch (ArrayStoreException e) {
            score += 20;
        }
        arr[1] = null; // null is always storable, whatever the element type
        if (arr[1] == null) {
            score += 12;
        }
        return score; // 42
    }
}
