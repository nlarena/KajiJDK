import java.beans.Encoder;
import java.beans.Expression;
import java.beans.Statement;

public class KajiBeansHumoS extends Encoder {
    static int prof = 0;
    static int emitidas = 0;
    static boolean corte = false;

    static void log(String s) {
        if (emitidas < 60) { emitidas++;
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < prof && i < 40; i++) { b.append('.'); }
            System.out.println(b + s);
        }
    }
    protected void writeObject(Object o) {
        if (corte) { return; }
        if (prof > 40) { corte = true; System.out.println("!!! CORTE por profundidad"); return; }
        prof++; log("obj " + (o == null ? "null" : o.getClass().getName()));
        super.writeObject(o); prof--;
    }
    public void writeStatement(Statement s) {
        if (corte) { return; }
        prof++; log("stm " + s); super.writeStatement(s); prof--;
    }
    public void writeExpression(Expression e) {
        if (corte) { return; }
        if (prof > 40) { corte = true; System.out.println("!!! CORTE por profundidad"); return; }
        prof++; log("exp " + e.getMethodName() + " target=" + nom(e.getTarget()));
        super.writeExpression(e); prof--;
    }
    static String nom(Object o) { return o == null ? "null" : o.getClass().getName(); }
    public void escribir(Object o) { this.writeObject(o); }

    public static int run() throws Exception {
        KajiBeansHumoS enc = new KajiBeansHumoS();
        enc.escribir(new XPunto());
        return -1;
    }
    public static void main(String[] x) throws Exception { System.out.println(run()); }
}
