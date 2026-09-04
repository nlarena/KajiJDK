import java.beans.Encoder;
import java.beans.Expression;
import java.beans.Statement;
import java.util.ArrayList;
import java.util.List;

public class KajiBeansHumoX extends Encoder {
    StringBuilder log = new StringBuilder();
    public void writeStatement(Statement s) {
        log.append("S:").append(s.getMethodName()).append("/").append(s.getArguments().length).append("\n");
        super.writeStatement(s);
    }
    public void writeExpression(Expression e) {
        log.append("E:").append(e.getMethodName()).append("\n");
        super.writeExpression(e);
    }
    public void escribir(Object o) { super.writeObject(o); }

    public static int run() throws Exception {
        List<Object> l = new ArrayList<Object>();
        l.add("x");
        l.add(l);
        KajiBeansHumoX enc = new KajiBeansHumoX();
        enc.setExceptionListener(new java.beans.ExceptionListener() {
            public void exceptionThrown(Exception e) {
                System.out.println("  LISTENER: " + e.getClass().getName());
            }
        });
        enc.escribir(l);
        System.out.println("codificado OK:\n" + enc.log);
        return -1;
    }
    public static void main(String[] x) throws Exception { System.out.println(run()); }
}
