import java.util.TimerTask;

// Semantica de TimerTask.cancel(): solo devuelve true si la tarea estaba SCHEDULED.
// Sin un Timer que la agende, se manipula `state` desde el mismo paquete no se puede,
// asi que se prueba lo observable desde afuera: VIRGIN -> cancel() false, y cancel()
// repetido tambien false. Y scheduledExecutionTime sobre period=0 devuelve
// nextExecutionTime (0 aca), o sea 0.
//
// Java real: 100.
//   cancel() sobre VIRGIN   -> false -> 0
//   cancel() otra vez       -> false -> 0
//   scheduledExecutionTime  -> 0
//   se devuelve 100 + c1*10 + c2 con c1=c2=0
public class TtTest {
    static class Nada extends TimerTask {
        public void run() { }
    }

    public static int run() {
        Nada t = new Nada();
        int c1 = t.cancel() ? 1 : 0;
        int c2 = t.cancel() ? 1 : 0;
        long s = t.scheduledExecutionTime();
        return 100 + c1 * 10 + c2 + (int) s;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
