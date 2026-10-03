// `instanceof` con deconstruccion de records (§14.30.1), las formas que se soportan.
//
// El bajado declara las variables del patron AFUERA del `if` con su valor por defecto, el match las
// ASIGNA, y el `instanceof` se reemplaza en la condicion por una bandera. Eso es lo que permite que
// `conGuarda` use `a` y `b` a la derecha de un `&&`. `unaVez` es la que custodia que el operando se
// evalue UNA sola vez: da 1, y daria 2 si se reevaluara --que es el bug que la forma simple
// (`e instanceof T v`) todavia tiene, anotado aparte--.
record Pt(int x, int y) {}
record Caja(Pt dentro, String etiqueta) {}

public class IofTest {
    static int llamadas = 0;
    static Object conEfecto() { IofTest.llamadas = IofTest.llamadas + 1; return new Pt(3, 4); }

    // La forma canonica, con else.
    static int simple(Object o) {
        if (o instanceof Pt(int a, int b)) return a + b;
        else return -1;
    }

    // La bandera con `&&`: la guarda usa las variables del patron.
    static int conGuarda(Object o) {
        if (o instanceof Pt(int a, int b) && a > b) return 1;
        return 0;
    }

    // `!` con las variables usadas ADENTRO del if (el idiom de guarda, que las usa despues, no
    // entra: el alcance de una variable de patron todavia es "el `if`" y no "donde la condicion es
    // verdadera" (§6.3.1), y eso falla igual con la forma simple. Anotado aparte).
    static int negada(Object o) {
        if (!(o instanceof Pt(int a, int b))) return -1;
        else return a * 100 + b;
    }

    // Deconstruccion anidada, con un componente de referencia.
    static String anidado(Object o) {
        if (o instanceof Caja(Pt(int a, int b), String et)) return et + ":" + (a + b);
        return "no";
    }

    // El operando se evalua UNA sola vez.
    static int unaVez() {
        IofTest.llamadas = 0;
        if (IofTest.conEfecto() instanceof Pt(int a, int b)) return IofTest.llamadas;
        return -1;
    }

    public static void main(String[] z) {
        System.out.println(simple(new Pt(3, 4)));
        System.out.println(simple("x"));
        System.out.println(conGuarda(new Pt(9, 2)));
        System.out.println(conGuarda(new Pt(2, 9)));
        System.out.println(conGuarda("x"));
        System.out.println(negada(new Pt(5, 6)));
        System.out.println(negada("x"));
        System.out.println(anidado(new Caja(new Pt(1, 2), "c")));
        System.out.println(anidado("x"));
        System.out.println(unaVez());
    }
}
