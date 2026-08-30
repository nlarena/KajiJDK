import java.util.Observable;
import java.util.Observer;

// Comportamiento de Observable/Observer, codificado en un int porque run-headless reporta el
// valor de retorno y no el stdout.
//
// `seq` acumula el orden de notificacion: cada observador hace seq = seq*10 + su id (A=1, B=2).
// El resultado final es seq*1000 + (obs tras alta duplicada)*100 + (obs tras baja)*10 + (obs
// tras deleteObservers).
//
// Java real: 212210.
//   - sin setChanged no notifica              -> seq sigue 0
//   - medir(): notifica en orden inverso B,A  -> seq = 21
//   - tras deleteObserver(a), medir(): B      -> seq = 212
//   - tras deleteObservers(), medir(): nada   -> seq = 212
//   - conteos: 2 (el alta duplicada no suma), 1, 0
public class ObsTest {

    static int seq;

    static class Sensor extends Observable {
        void medir() {
            setChanged();
            notifyObservers();
        }
        void medirSinMarcar() {
            notifyObservers();
        }
    }

    static class Registro implements Observer {
        int id;
        Registro(int id) { this.id = id; }
        public void update(Observable o, Object arg) {
            ObsTest.seq = ObsTest.seq * 10 + this.id;
        }
    }

    public static int run() {
        seq = 0;
        Sensor s = new Sensor();
        Registro a = new Registro(1);
        Registro b = new Registro(2);

        s.addObserver(a);
        s.addObserver(b);
        s.addObserver(a);
        int c1 = s.countObservers();

        s.medirSinMarcar();
        s.medir();

        s.deleteObserver(a);
        int c2 = s.countObservers();
        s.medir();

        s.deleteObservers();
        int c3 = s.countObservers();
        s.medir();

        return seq * 1000 + c1 * 100 + c2 * 10 + c3;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
