package pb;
import pa.Gen;
// generico (cast), metodo generico, varargs, tipo de firma NO importado, byte con constante, arreglo
public class P13 extends Gen<String> {
    static class N {
        void u(P13 h) {
            h.val = "v"; String s = h.val; String g = h.get(); Integer i = h.id(7);
            int v0 = h.va("ab"); int v3 = h.va("ab", 1, 2, 3); int va = h.va("x", new int[]{4, 5});
            String sec = h.conSecreto(pa.Secreto.crear());
            h.b = 5; h.b += 300; h.arr[1] = 9; h.arr[0]++;
            String sv = sva(1, "x", null) + Gen.sva();
            System.out.println("P13 " + s + g + i + " " + v0 + " " + v3 + " " + va + " " + sec + " " + h.b + " " + h.arr[0] + h.arr[1] + " " + sv);
        }
    }
    public static void main(String[] a) { new N().u(new P13()); }
}
