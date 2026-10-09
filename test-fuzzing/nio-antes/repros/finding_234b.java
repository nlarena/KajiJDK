// La otra mitad de #234 - ver finding_234.java.
public class finding_234b {

    static final int SEMILLA = 41;

    static boolean impar(int n) {
        if (n == 0) { return false; }
        return finding_234.par(n - 1);
    }
}
