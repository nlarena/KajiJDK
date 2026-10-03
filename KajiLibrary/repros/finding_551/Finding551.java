/**
 * Finding #551: un cast de un primitivo a un tipo referencia (`(Object) 1`, `(Integer) 4`) no boxea
 * (§5.5: la conversion de cast incluye el boxing). Se emite el `checkcast` sobre el `int` crudo: la
 * HotSpot lo rechaza con un `VerifyError` y nuestra VM revienta con
 * "checkcast: expected an object reference". Sin el cast (`Object o = 5;`) anda.
 */
public class Finding551 {
    public static int run() {
        Object o = (Object) 1;
        Integer i = (Integer) 4;
        System.out.println("//551 " + o + " " + i);
        return i;
    }

    public static void main(String[] args) { run(); }
}
