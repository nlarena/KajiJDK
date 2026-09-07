/**
 * La invocacion calificada del constructor de la superclase: {@code externa.super(...)}.
 *
 * <p>Es la forma que hace falta cuando una clase extiende a una clase <em>interna</em> de otra y no
 * esta adentro de ella: hay que decirle cual es la instancia externa (JLS 8.8.7.1). El JDK la
 * compila; este compilador ni siquiera la analiza -- falla en el analizador, con
 * "se esperaba un identificador, se encontro Super".
 */
public class Finding518 {

    static class Externa {

        /** Interna, no estatica: quien la extienda necesita una instancia externa. */
        class Interna {
            Interna(int n) {
            }
        }
    }

    /** Extiende una clase interna desde afuera de su externa. */
    static class Hija extends Externa.Interna {

        Hija(Externa e) {
            e.super(1);
        }
    }

    public static void main(String[] a) {
        System.out.println(new Hija(new Externa()));
    }
}
