import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import javax.security.auth.AuthPermission;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.security.auth.PrivateCredentialPermission;
import javax.security.auth.RefreshFailedException;
import javax.security.auth.Refreshable;
import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;

/**
 * javax.security.auth: Subject, los dos permisos, el combinador y las dos interfaces chicas.
 *
 * <p>Las expectativas salieron de preguntarle al JDK 25, no de leer la documentacion. Las cuatro que
 * no son obvias y por las que vale la pena tener la prueba: el conjunto de identidades tira
 * SecurityException --no ClassCastException-- ante algo que no es un Principal, pero el constructor
 * de cuatro no chequea nada; callAs envuelve hasta las excepciones de ejecucion; doAs sigue atando
 * el Subject aunque este desaconsejado; y en PrivateCredentialPermission el permiso con MENOS
 * principals implica al que tiene mas, no al reves.
 */
public class AuthTest {

    static class P implements Principal {
        String n;

        P(String n) { this.n = n; }

        public String getName() { return n; }

        public boolean equals(Object o) {
            return o instanceof P && ((P) o).n.equals(n) && o.getClass() == getClass();
        }

        public int hashCode() { return n.hashCode(); }

        public String toString() { return "P(" + n + ")"; }
    }

    static class Q extends P {
        Q(String n) { super(n); }

        public String toString() { return "Q(" + n + ")"; }
    }

    /** Para mirar los defaults de Destroyable y comprobar que son los seguros. */
    static class PlainDestroyable implements Destroyable {
    }

    /** Devuelve el Subject atado al hilo. */
    static class CurrentSubject implements Callable<Subject> {
        public Subject call() { return Subject.current(); }
    }

    /**
     * Un callAs adentro de otro: comprueba que el de adentro tapa al de afuera y que al salir vuelve
     * el de afuera.
     *
     * <p>Va como clase nombrada y no como dos Callable anonimos anidados porque nuestro javac no
     * emite una clase anonima declarada adentro de otra -- ver el finding #470.
     */
    static class Nested implements Callable<String> {
        Subject outerSubject;
        Subject innerSubject;

        Nested(Subject outerSubject, Subject innerSubject) {
            this.outerSubject = outerSubject;
            this.innerSubject = innerSubject;
        }

        public String call() {
            Subject seen = Subject.callAs(innerSubject, new CurrentSubject());
            return (seen == innerSubject ? "interno" : "mal") + "/"
                + (Subject.current() == outerSubject ? "vuelve" : "mal");
        }
    }

    static class OneRefreshable implements Refreshable {
        boolean current = false;

        public boolean isCurrent() { return current; }

        public void refresh() throws RefreshFailedException {
            throw new RefreshFailedException("no hay de donde");
        }
    }

    public static int run() {
        int i = 0;

        // ======================================================================================
        // Subject: los conjuntos son vistas vivas
        // ======================================================================================
        Subject s = new Subject();
        if (!s.getPrincipals().isEmpty()) { return i; } i++;
        if (s.isReadOnly()) { return i; } i++;
        // Agregar a lo que devuelve getPrincipals() agrega AL SUBJECT: no es una copia.
        s.getPrincipals().add(new P("juan"));
        if (s.getPrincipals().size() != 1) { return i; } i++;
        // Y una vista pedida antes ve lo que se agrego despues.
        Set<Principal> view = s.getPrincipals();
        s.getPrincipals().add(new P("ana"));
        if (view.size() != 2) { return i; } i++;
        if (!view.contains(new P("juan"))) { return i; } i++;
        if (!view.remove(new P("ana"))) { return i; } i++;
        if (s.getPrincipals().size() != 1) { return i; } i++;
        // Repetido no entra dos veces.
        s.getPrincipals().add(new P("juan"));
        if (s.getPrincipals().size() != 1) { return i; } i++;

        // Algo que no es Principal: SecurityException, no ClassCastException. El generico se borra,
        // asi que el conjunto se defiende en ejecucion.
        boolean threw = false;
        try {
            Set raw = new Subject().getPrincipals();
            raw.add("no soy un Principal");
        } catch (SecurityException e) {
            threw = true;
        }
        if (!threw) { return i; } i++;
        threw = false;
        try { new Subject().getPrincipals().add(null); }
        catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Las credenciales, en cambio, aceptan cualquier cosa: son Object a proposito.
        Subject cred = new Subject();
        cred.getPublicCredentials().add("un texto");
        cred.getPrivateCredentials().add(Integer.valueOf(7));
        if (cred.getPublicCredentials().size() != 1) { return i; } i++;
        if (cred.getPrivateCredentials().size() != 1) { return i; } i++;

        // ======================================================================================
        // setReadOnly es de ida sola
        // ======================================================================================
        Subject ro = new Subject();
        ro.getPrincipals().add(new P("juan"));
        ro.setReadOnly();
        if (!ro.isReadOnly()) { return i; } i++;
        threw = false;
        try { ro.getPrincipals().add(new P("otro")); }
        catch (IllegalStateException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { ro.getPrincipals().remove(new P("juan")); }
        catch (IllegalStateException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Tambien por el iterador: es la otra puerta al mismo conjunto.
        threw = false;
        try {
            Iterator<Principal> it = ro.getPrincipals().iterator();
            it.next();
            it.remove();
        } catch (IllegalStateException e) {
            threw = true;
        }
        if (!threw) { return i; } i++;
        // Leer sigue andando.
        if (ro.getPrincipals().size() != 1) { return i; } i++;

        // ======================================================================================
        // el constructor de cuatro copia los conjuntos y NO chequea los tipos
        // ======================================================================================
        Set<Principal> ps = new HashSet<Principal>();
        ps.add(new P("ana"));
        Set<Object> pub = new HashSet<Object>();
        pub.add("publica");
        Set<Object> priv = new HashSet<Object>();
        priv.add("privada");
        Subject c4 = new Subject(true, ps, pub, priv);
        if (!c4.isReadOnly()) { return i; } i++;
        if (c4.getPrincipals().size() != 1) { return i; } i++;
        if (c4.getPublicCredentials().size() != 1) { return i; } i++;
        // Se copian: tocar el conjunto de afuera no cambia el Subject.
        ps.add(new P("colado"));
        if (c4.getPrincipals().size() != 1) { return i; } i++;
        threw = false;
        try { new Subject(false, null, new HashSet<Object>(), new HashSet<Object>()); }
        catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Y esta es la rareza: por esta puerta un elemento que no es Principal entra sin protestar.
        boolean passed = false;
        try {
            Set rawBytes = new HashSet();
            rawBytes.add("no soy un Principal");
            new Subject(false, rawBytes, new HashSet<Object>(), new HashSet<Object>());
            passed = true;
        } catch (Exception e) {
            passed = false;
        }
        if (!passed) { return i; } i++;

        // ======================================================================================
        // getPrincipals(Class) y las dos de credenciales: COPIA, no vista
        // ======================================================================================
        Subject g = new Subject();
        g.getPrincipals().add(new P("base"));
        g.getPrincipals().add(new Q("derivada"));
        if (g.getPrincipals(P.class).size() != 2) { return i; } i++;
        if (g.getPrincipals(Q.class).size() != 1) { return i; } i++;
        // Agregar a lo que sale de aca NO agrega al Subject: al reves que getPrincipals().
        g.getPrincipals(P.class).add(new P("z"));
        if (g.getPrincipals().size() != 2) { return i; } i++;
        threw = false;
        try { g.getPrincipals(null); }
        catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;
        Subject gc = new Subject();
        gc.getPublicCredentials().add("texto");
        gc.getPublicCredentials().add(Integer.valueOf(7));
        if (gc.getPublicCredentials(String.class).size() != 1) { return i; } i++;
        if (gc.getPublicCredentials(Integer.class).size() != 1) { return i; } i++;
        // Lo publico no aparece en lo privado: son dos conjuntos distintos.
        if (!gc.getPrivateCredentials(String.class).isEmpty()) { return i; } i++;

        // ======================================================================================
        // equals, hashCode y toString
        // ======================================================================================
        Subject a1 = new Subject();
        a1.getPrincipals().add(new P("juan"));
        Subject a2 = new Subject();
        a2.getPrincipals().add(new P("juan"));
        if (!a1.equals(a2)) { return i; } i++;
        if (a1.hashCode() != a2.hashCode()) { return i; } i++;
        a2.getPublicCredentials().add("algo");
        if (a1.equals(a2)) { return i; } i++;
        if (a1.equals(null)) { return i; } i++;
        if (a1.equals("x")) { return i; } i++;
        if (!a1.equals(a1)) { return i; } i++;
        // El formato de toString sale del JDK: una linea por elemento, con tabulacion.
        if (!new Subject().toString().equals("Subject:\n")) { return i; } i++;
        Subject t = new Subject();
        t.getPrincipals().add(new P("juan"));
        t.getPublicCredentials().add("cp");
        t.getPrivateCredentials().add("cv");
        if (!t.toString().equals("Subject:\n\tPrincipal: P(juan)\n"
                + "\tPublic Credential: cp\n\tPrivate Credential: cv\n")) { return i; } i++;

        // ======================================================================================
        // current, callAs, doAs
        // ======================================================================================
        if (Subject.current() != null) { return i; } i++;
        final Subject actor = new Subject();
        actor.getPrincipals().add(new P("actuando"));
        Subject seen = Subject.callAs(actor, new Callable<Subject>() {
            public Subject call() { return Subject.current(); }
        });
        if (seen != actor) { return i; } i++;
        // Y al salir se restaura: no queda atado.
        if (Subject.current() != null) { return i; } i++;

        // Anidado: el de adentro tapa al de afuera, y al volver vuelve el de afuera.
        final Subject innerRun = new Subject();
        innerRun.getPrincipals().add(new P("interno"));
        String nested = Subject.callAs(actor, new Nested(actor, innerRun));
        if (!nested.equals("interno/vuelve")) { return i; } i++;

        // Un subject null es legitimo: adentro current() da null.
        Subject nulled = Subject.callAs(null, new CurrentSubject());
        if (nulled != null) { return i; } i++;

        // callAs envuelve CUALQUIER excepcion, las de ejecucion incluidas.
        Throwable cause = null;
        try {
            Subject.callAs(actor, new Callable<String>() {
                public String call() { throw new IllegalStateException("ay"); }
            });
        } catch (CompletionException e) {
            cause = e.getCause();
        }
        if (cause == null || !(cause instanceof IllegalStateException)) { return i; } i++;
        // Y aun asi el Subject se desata: el finally corre igual.
        if (Subject.current() != null) { return i; } i++;
        threw = false;
        try { Subject.callAs(actor, null); }
        catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;

        // doAs esta desaconsejado pero sigue atando el Subject.
        Subject byDoAs = Subject.doAs(actor, new PrivilegedAction<Subject>() {
            public Subject run() { return Subject.current(); }
        });
        if (byDoAs != actor) { return i; } i++;
        if (Subject.current() != null) { return i; } i++;
        threw = false;
        try { Subject.doAs(actor, (PrivilegedAction<String>) null); }
        catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;
        try {
            String v = Subject.doAs(actor, new PrivilegedExceptionAction<String>() {
                public String run() { return "pea"; }
            });
            if (!v.equals("pea")) { return i; }
            i++;
        } catch (PrivilegedActionException e) {
            return i;
        }
        // A diferencia de callAs, aca solo se envuelven las excepciones DECLARADAS.
        cause = null;
        try {
            Subject.doAs(actor, new PrivilegedExceptionAction<String>() {
                public String run() throws Exception { throw new java.io.IOException("ay"); }
            });
        } catch (PrivilegedActionException e) {
            cause = e.getCause();
        }
        if (cause == null || !(cause instanceof java.io.IOException)) { return i; } i++;
        // Un RuntimeException sale tal cual, sin envolver.
        threw = false;
        try {
            Subject.doAs(actor, new PrivilegedExceptionAction<String>() {
                public String run() { throw new IllegalStateException("crudo"); }
            });
        } catch (IllegalStateException e) {
            threw = true;
        } catch (Exception e) {
            threw = false;
        }
        if (!threw) { return i; } i++;
        Subject byDoAsPrivileged = Subject.doAsPrivileged(actor, new PrivilegedAction<Subject>() {
            public Subject run() { return Subject.current(); }
        }, null);
        if (byDoAsPrivileged != actor) { return i; } i++;

        // getSubject ya no anda: lanza en vez de devolver null, que se leeria como una respuesta.
        threw = false;
        try { Subject.getSubject(null); }
        catch (UnsupportedOperationException e) { threw = true; }
        if (!threw) { return i; } i++;

        // ======================================================================================
        // AuthPermission
        // ======================================================================================
        AuthPermission ap = new AuthPermission("doAs");
        if (!ap.getName().equals("doAs")) { return i; } i++;
        if (!ap.getActions().equals("")) { return i; } i++;
        if (!ap.implies(new AuthPermission("doAs"))) { return i; } i++;
        if (ap.implies(new AuthPermission("otro"))) { return i; } i++;
        if (!new AuthPermission("*").implies(ap)) { return i; } i++;
        if (!new AuthPermission("a.*").implies(new AuthPermission("a.b"))) { return i; } i++;
        if (new AuthPermission("a.b").implies(new AuthPermission("a.*"))) { return i; } i++;
        if (!ap.equals(new AuthPermission("doAs"))) { return i; } i++;
        if (!new AuthPermission("doAs", "ignorado").getActions().equals("")) { return i; } i++;
        // La traduccion historica: el nombre pelado se guarda como el comodin.
        if (!new AuthPermission("createLoginContext").getName()
                .equals("createLoginContext.*")) { return i; } i++;
        if (!new AuthPermission("createLoginContext")
                .implies(new AuthPermission("createLoginContext.Kaji"))) { return i; } i++;

        // ======================================================================================
        // PrivateCredentialPermission
        // ======================================================================================
        PrivateCredentialPermission onePrincipal = new PrivateCredentialPermission(
            "java.lang.String P1 \"n1\"", "read");
        if (!onePrincipal.getCredentialClass().equals("java.lang.String")) { return i; } i++;
        if (!onePrincipal.getActions().equals("read")) { return i; } i++;
        if (!onePrincipal.getName().equals("java.lang.String P1 \"n1\"")) { return i; } i++;
        String[][] pr = onePrincipal.getPrincipals();
        if (pr.length != 1 || !pr[0][0].equals("P1") || !pr[0][1].equals("n1")) { return i; } i++;
        // Es una copia: tocarla no cambia el permiso.
        pr[0][0] = "roto";
        if (!onePrincipal.getPrincipals()[0][0].equals("P1")) { return i; } i++;
        // Las acciones: solo "read", en cualquier caja.
        if (!new PrivateCredentialPermission("C P \"n\"", "READ").getActions()
                .equals("read")) { return i; } i++;
        threw = false;
        try { new PrivateCredentialPermission("C P \"n\"", "write"); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new PrivateCredentialPermission("C P \"n\"", null); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;

        // La sintaxis del nombre.
        threw = false;
        try { new PrivateCredentialPermission("", "read"); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new PrivateCredentialPermission("java.lang.String", "read"); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new PrivateCredentialPermission("java.lang.String P a", "read"); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Clase de principal comodin con nombre concreto: no se puede evaluar, se rechaza.
        threw = false;
        try { new PrivateCredentialPermission("C * \"n\"", "read"); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Con el nombre tambien comodin si vale.
        passed = false;
        try { new PrivateCredentialPermission("C * \"*\"", "read"); passed = true; }
        catch (Exception e) { passed = false; }
        if (!passed) { return i; } i++;
        // Y la clase de credencial comodin sola tambien.
        passed = false;
        try { new PrivateCredentialPermission("* P \"n\"", "read"); passed = true; }
        catch (Exception e) { passed = false; }
        if (!passed) { return i; } i++;

        // implies: el que tiene MENOS principals implica al que tiene mas.
        PrivateCredentialPermission twoPrincipals = new PrivateCredentialPermission(
            "java.lang.String P1 \"n1\" P2 \"n2\"", "read");
        if (!onePrincipal.implies(twoPrincipals)) { return i; } i++;
        if (twoPrincipals.implies(onePrincipal)) { return i; } i++;
        if (!onePrincipal.implies(onePrincipal)) { return i; } i++;
        if (onePrincipal.implies(null)) { return i; } i++;
        if (onePrincipal.implies(new AuthPermission("x"))) { return i; } i++;
        // Distinta clase de credencial: no implica.
        if (onePrincipal.implies(new PrivateCredentialPermission("otra P1 \"n1\"", "read"))) { return i; } i++;
        if (!new PrivateCredentialPermission("* P1 \"n1\"", "read").implies(onePrincipal)) { return i; } i++;
        // Nombre de principal comodin.
        if (!new PrivateCredentialPermission("java.lang.String P1 \"*\"", "read")
                .implies(onePrincipal)) { return i; } i++;
        if (!new PrivateCredentialPermission("java.lang.String * \"*\"", "read")
                .implies(onePrincipal)) { return i; } i++;
        // equals ignora el orden de los pares, y el hash tiene que ser consistente con eso.
        PrivateCredentialPermission reversed = new PrivateCredentialPermission(
            "java.lang.String P2 \"n2\" P1 \"n1\"", "read");
        if (!twoPrincipals.equals(reversed)) { return i; } i++;
        if (twoPrincipals.hashCode() != reversed.hashCode()) { return i; } i++;
        if (twoPrincipals.equals(onePrincipal)) { return i; } i++;
        if (!twoPrincipals.equals(twoPrincipals)) { return i; } i++;
        if (twoPrincipals.equals(null)) { return i; } i++;
        if (twoPrincipals.newPermissionCollection() != null) { return i; } i++;
        // Un nombre con espacios adentro de las comillas es un nombre solo.
        PrivateCredentialPermission withSpace = new PrivateCredentialPermission(
            "C P \"cn=Juan Perez\"", "read");
        if (!withSpace.getPrincipals()[0][1].equals("cn=Juan Perez")) { return i; } i++;

        // ======================================================================================
        // SubjectDomainCombiner
        // ======================================================================================
        SubjectDomainCombiner sdc = new SubjectDomainCombiner(actor);
        if (sdc.getSubject() != actor) { return i; } i++;
        // Sin dominios actuales devuelve los asignados TAL CUAL, null incluido.
        if (sdc.combine(null, null) != null) { return i; } i++;
        java.security.ProtectionDomain[] assigned = new java.security.ProtectionDomain[1];
        assigned[0] = new java.security.ProtectionDomain(null, null);
        if (sdc.combine(null, assigned) != assigned) { return i; } i++;
        if (sdc.combine(new java.security.ProtectionDomain[0], assigned)
                != assigned) { return i; } i++;
        java.security.ProtectionDomain[] current = new java.security.ProtectionDomain[2];
        // El de dos argumentos es de permisos ESTATICOS; el de cuatro es dinamico.
        current[0] = new java.security.ProtectionDomain(null, null);
        current[1] = new java.security.ProtectionDomain(null, null, null, null);
        if (!current[0].staticPermissionsOnly()) { return i; } i++;
        if (current[1].staticPermissionsOnly()) { return i; } i++;
        java.security.ProtectionDomain[] mixed = sdc.combine(current, assigned);
        if (mixed.length != 3) { return i; } i++;
        // Al estatico no le cambia nada tener identidades encima: vuelve la misma instancia.
        if (mixed[0] != current[0]) { return i; } i++;
        // El dinamico si se rehace, y con las identidades del Subject.
        if (mixed[1] == current[1]) { return i; } i++;
        if (mixed[1].getPrincipals().length != 1) { return i; } i++;
        if (!mixed[1].getPrincipals()[0].getName().equals("actuando")) { return i; } i++;
        // Los asignados van tal cual atras.
        if (mixed[2] != assigned[0]) { return i; } i++;

        // ======================================================================================
        // Destroyable y Refreshable
        // ======================================================================================
        Destroyable d = new PlainDestroyable();
        // Los defaults son los seguros: dice que no sabe borrar en vez de mentir que borro.
        if (d.isDestroyed()) { return i; } i++;
        threw = false;
        try { d.destroy(); }
        catch (DestroyFailedException e) { threw = true; }
        if (!threw) { return i; } i++;
        if (new DestroyFailedException("x").getMessage() == null) { return i; } i++;
        if (new DestroyFailedException().getMessage() != null) { return i; } i++;

        OneRefreshable r = new OneRefreshable();
        if (r.isCurrent()) { return i; } i++;
        threw = false;
        try { r.refresh(); }
        catch (RefreshFailedException e) { threw = "no hay de donde".equals(e.getMessage()); }
        if (!threw) { return i; } i++;
        if (new RefreshFailedException().getMessage() != null) { return i; } i++;

        // ======================================================================================
        // Principal.implies(Subject): el default es la respuesta chica
        // ======================================================================================
        P juan = new P("juan");
        if (juan.implies(null)) { return i; } i++;
        if (juan.implies(new Subject())) { return i; } i++;
        Subject conJuan = new Subject();
        conJuan.getPrincipals().add(new P("juan"));
        if (!juan.implies(conJuan)) { return i; } i++;
        Subject conOtro = new Subject();
        conOtro.getPrincipals().add(new P("ana"));
        if (juan.implies(conOtro)) { return i; } i++;
        // Alcanza con que este entre los suyos; no hace falta que sea el unico.
        Subject conLosDos = new Subject();
        conLosDos.getPrincipals().add(new P("ana"));
        conLosDos.getPrincipals().add(new P("juan"));
        if (!juan.implies(conLosDos)) { return i; } i++;

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
