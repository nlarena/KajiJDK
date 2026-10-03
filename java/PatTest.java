// Las cinco formas del `switch` sobre patrones (Hito B7), con sus valores observados.
//
// Los tipos llevan prefijo `Pat` a proposito: `Shape`/`Circle`/`Square` ya existen en `java/`
// (la jerarquia sellada de `Shape.java`, que usan `Sealed`, `JcShapes`, `BmVirtual` y `VSX`), y
// `--emit` escribe el `.class` al lado de la fuente, asi que reusar esos nombres les pisa el suyo.
sealed interface PatShape permits PatCircle, PatSquare, PatHole {}
record PatCircle(int r) implements PatShape {}
record PatSquare(int side) implements PatShape {}
record PatHole(PatShape inner) implements PatShape {}

public class PatTest {
    // Expresion con default, y un brazo que devuelve la variable de patron: pide desboxeo.
    static int unbox(Object o) {
        return switch (o) { case Integer i -> i; case String s -> s.length(); default -> -1; };
    }

    // Expresion EXHAUSTIVA sin default sobre un sealed: el `default` lo pone la spec.
    static int area(PatShape s) {
        return switch (s) {
            case PatCircle c -> 3 * c.r() * c.r();
            case PatSquare q -> q.side() * q.side();
            case PatHole h -> -1;
        };
    }

    // Guardas `when`, en cascada sobre el mismo tipo.
    static String size(Object o) {
        return switch (o) {
            case Integer i when i > 100 -> "grande";
            case Integer i when i > 10 -> "mediano";
            case Integer i -> "chico";
            default -> "otro";
        };
    }

    // Deconstruccion de records, con un nivel anidado y componentes primitivos.
    static int deep(PatShape s) {
        return switch (s) {
            case PatCircle(int r) -> r;
            case PatSquare(int side) -> side * 10;
            case PatHole(PatCircle(int r)) -> 1000 + r;
            case PatHole(PatShape other) -> 2000;
        };
    }

    // La forma SENTENCIA, tambien sin default.
    static int stmt(PatShape s) {
        int acc = 0;
        switch (s) {
            case PatCircle c -> acc = 1;
            case PatSquare q -> acc = 2;
            case PatHole h -> acc = 3;
        }
        return acc;
    }

    public static void main(String[] a) {
        System.out.println(unbox(7));
        System.out.println(unbox("hola"));
        System.out.println(unbox(2.5));
        System.out.println(area(new PatCircle(2)));
        System.out.println(area(new PatSquare(5)));
        System.out.println(size(500));
        System.out.println(size(50));
        System.out.println(size(5));
        System.out.println(size("x"));
        System.out.println(deep(new PatCircle(9)));
        System.out.println(deep(new PatSquare(3)));
        System.out.println(deep(new PatHole(new PatCircle(4))));
        System.out.println(deep(new PatHole(new PatSquare(1))));
        System.out.println(stmt(new PatCircle(1)));
        System.out.println(stmt(new PatHole(new PatCircle(1))));
    }
}
