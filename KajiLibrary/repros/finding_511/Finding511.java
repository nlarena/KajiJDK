// #511 -- en una clase interna, `this(...)` cuenta la instancia externa como argumento y puede
// resolver al constructor equivocado; con dos constructores encadenados, a si mismo.
//
// El `.class` sale sin una palabra y el programa se cuelga con StackOverflowError en el primer
// `new`. Es la peor forma de un fallo: silencioso al compilar y lejos del origen al ejecutar.
public class Finding511 {

    // --- falla ---------------------------------------------------------------------------------

    class Interna {
        String quien;

        Interna(String a, Object b) {
            quien = "dos argumentos";
        }

        Interna(Object b) {
            this(null, b);        // debe llamar al de dos; llama a este mismo
        }
    }

    // --- anda: la misma forma en una clase anidada estatica -------------------------------------

    static class Anidada {
        String quien;

        Anidada(String a, Object b) {
            quien = "dos argumentos";
        }

        Anidada(Object b) {
            this(null, b);
        }
    }

    // --- anda: la misma forma en una clase de nivel superior (ver Companera, abajo) -------------

    public static int run() {
        System.out.println("//anidada " + new Anidada(new Object()).quien);
        System.out.println("//companera " + new Companera(new Object()).quien);
        System.out.println("//interna " + new Finding511().crear().quien);
        return 0;
    }

    Interna crear() {
        return new Interna(new Object());
    }

    public static void main(String[] a) {
        run();
    }
}

class Companera {
    String quien;

    Companera(String a, Object b) {
        quien = "dos argumentos";
    }

    Companera(Object b) {
        this(null, b);
    }
}
