import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;

// Prueba de comportamiento de `StampedLock`. Compilada contra el del JDK y corrida sobre el
// nuestro, asi que las dos VMs tienen que dar lo mismo.
//
// Devuelve **-1 si esta todo bien**; si no, el numero del paso que fallo.
//
// El paso que mas importa es el 3 y el 12: **el sello de la lectura optimista se valida de
// verdad**. Un `tryOptimisticRead()` que devolviera un numero que despues siempre valida --o que
// nunca valida-- pasaria un test de tipos y no seria un lock. El 12 lo prueba con escritores de
// verdad encima: el lector optimista lee dos campos que el escritor mantiene iguales, y si el
// sello mintiera alguna vez los veria distintos.

class StampedWriter extends Thread {
    StampedLock lock;
    int[] compartido;
    int vueltas;

    public void run() {
        for (int i = 0; i < this.vueltas; i++) {
            long s = this.lock.writeLock();
            this.compartido[0] = this.compartido[0] + 1;
            this.compartido[1] = this.compartido[0];
            this.lock.unlockWrite(s);
        }
    }
}

class StampedOptReader extends Thread {
    StampedLock lock;
    int[] compartido;
    int vueltas;
    // Cuantas veces vio los dos campos distintos habiendo validado el sello: tiene que ser cero.
    volatile int rotas;
    // Cuantas lecturas optimistas valieron. Tienen que ser `vueltas`: cada vuelta reintenta
    // hasta conseguir una limpia, y el escritor termina, asi que siempre lo consigue.
    volatile int limpias;
    // 1 si alguna vuelta agoto los reintentos sin validar nunca.
    volatile int sinSuerte;

    public void run() {
        for (int i = 0; i < this.vueltas; i++) {
            boolean listo = false;
            for (int t = 0; t < 1000000 && !listo; t++) {
                long s = this.lock.tryOptimisticRead();
                int a = this.compartido[0];
                Thread.yield();
                int b = this.compartido[1];
                if (s != 0L && this.lock.validate(s)) {
                    listo = true;
                    this.limpias++;
                    if (a != b) {
                        this.rotas++;
                    }
                }
            }
            if (!listo) {
                this.sinSuerte = 1;
            }
        }
    }
}

public class StampedTest {

    public static int run() throws Exception {
        StampedLock lock = new StampedLock();

        // --- 1. Escritura ---------------------------------------------------------------------
        long w = lock.writeLock();
        if (w == 0L) {
            return 1;
        }
        if (!lock.isWriteLocked() || lock.isReadLocked()) {
            return 2;
        }
        if (!StampedLock.isWriteLockStamp(w) || !StampedLock.isLockStamp(w)) {
            return 3;
        }
        if (StampedLock.isReadLockStamp(w) || StampedLock.isOptimisticReadStamp(w)) {
            return 4;
        }
        // Con la escritura tomada no hay lectura optimista posible.
        if (lock.tryOptimisticRead() != 0L) {
            return 5;
        }
        if (lock.tryWriteLock() != 0L || lock.tryReadLock() != 0L) {
            return 6;
        }
        lock.unlockWrite(w);
        if (lock.isWriteLocked()) {
            return 7;
        }

        // --- 2. Lectura optimista: el sello vale hasta que alguien escribe --------------------
        long o = lock.tryOptimisticRead();
        if (o == 0L || !StampedLock.isOptimisticReadStamp(o) || StampedLock.isLockStamp(o)) {
            return 8;
        }
        if (!lock.validate(o)) {
            return 9;
        }
        // Tomar y soltar una LECTURA no invalida el sello: no cambio nada.
        long r1 = lock.readLock();
        if (!lock.validate(o)) {
            return 10;
        }
        lock.unlockRead(r1);
        // Tomar y soltar una ESCRITURA si lo invalida, y ese es todo el punto de la clase.
        long w2 = lock.writeLock();
        lock.unlockWrite(w2);
        if (lock.validate(o)) {
            return 11;
        }
        // Y el sello cero nunca vale.
        if (lock.validate(0L)) {
            return 12;
        }

        // --- 3. Varios lectores a la vez ------------------------------------------------------
        long ra = lock.readLock();
        long rb = lock.readLock();
        if (!lock.isReadLocked() || lock.isWriteLocked()) {
            return 13;
        }
        if (lock.getReadLockCount() != 2) {
            return 14;
        }
        if (!StampedLock.isReadLockStamp(ra) || !StampedLock.isLockStamp(ra)) {
            return 15;
        }
        if (StampedLock.isWriteLockStamp(ra)) {
            return 16;
        }
        if (lock.tryWriteLock() != 0L) {
            return 17; // con lectores adentro no puede entrar un escritor
        }
        lock.unlockRead(ra);
        if (lock.getReadLockCount() != 1) {
            return 18;
        }
        lock.unlock(rb); // el despachador generico
        if (lock.getReadLockCount() != 0 || lock.isReadLocked()) {
            return 19;
        }
        long w3 = lock.tryWriteLock();
        if (w3 == 0L) {
            return 20;
        }
        lock.unlock(w3);

        // --- 4. Conversiones ------------------------------------------------------------------
        long we = lock.writeLock();
        long haciaLectura = lock.tryConvertToReadLock(we);
        if (haciaLectura == 0L) {
            return 21;
        }
        if (lock.isWriteLocked() || !lock.isReadLocked()) {
            return 22;
        }
        lock.unlockRead(haciaLectura);

        long rl = lock.readLock();
        long haciaEscritura = lock.tryConvertToWriteLock(rl); // unico lector: se puede
        if (haciaEscritura == 0L) {
            return 23;
        }
        if (!lock.isWriteLocked() || lock.isReadLocked()) {
            return 24;
        }
        long haciaOptimista = lock.tryConvertToOptimisticRead(haciaEscritura);
        if (haciaOptimista == 0L) {
            return 25;
        }
        if (lock.isWriteLocked() || lock.isReadLocked()) {
            return 26;
        }
        if (!lock.validate(haciaOptimista)) {
            return 27;
        }
        // Dos lectores: ya no se puede pasar a escritura.
        long d1 = lock.readLock();
        long d2 = lock.readLock();
        if (lock.tryConvertToWriteLock(d1) != 0L) {
            return 28;
        }
        lock.unlockRead(d1);
        lock.unlockRead(d2);

        // --- 5. Los desbloqueos sin sello -----------------------------------------------------
        if (lock.tryUnlockWrite() || lock.tryUnlockRead()) {
            return 29; // no hay nada tomado: los dos tienen que decir que no
        }
        lock.writeLock();
        if (!lock.tryUnlockWrite()) {
            return 30;
        }
        lock.readLock();
        if (!lock.tryUnlockRead()) {
            return 31;
        }

        // --- 6. Los plazos --------------------------------------------------------------------
        long tw = lock.tryWriteLock(20L, TimeUnit.MILLISECONDS);
        if (tw == 0L) {
            return 32;
        }
        // Con la escritura tomada, un plazo de lectura tiene que vencer y devolver cero.
        if (lock.tryReadLock(20L, TimeUnit.MILLISECONDS) != 0L) {
            return 33;
        }
        lock.unlockWrite(tw);
        long tr = lock.tryReadLock(20L, TimeUnit.MILLISECONDS);
        if (tr == 0L) {
            return 34;
        }
        lock.unlockRead(tr);

        // --- 7. Las vistas `Lock` -------------------------------------------------------------
        Lock vw = lock.asWriteLock();
        Lock vr = lock.asReadLock();
        ReadWriteLock vle = lock.asReadWriteLock();
        if (vle.readLock() != vr || vle.writeLock() != vw) {
            return 35;
        }
        vw.lock();
        if (!lock.isWriteLocked()) {
            return 36;
        }
        if (vr.tryLock()) {
            return 37;
        }
        vw.unlock();
        if (!vr.tryLock()) {
            return 38;
        }
        if (lock.getReadLockCount() != 1) {
            return 39;
        }
        vr.unlock();
        boolean sinCondiciones = false;
        try {
            vw.newCondition();
        } catch (UnsupportedOperationException e) {
            sinCondiciones = true;
        }
        if (!sinCondiciones) {
            return 40;
        }

        // --- 8. Sellos mal usados -------------------------------------------------------------
        long buena = lock.writeLock();
        boolean protesto = false;
        try {
            lock.unlockRead(buena); // un sello de escritura no sirve para soltar una lectura
        } catch (IllegalMonitorStateException e) {
            protesto = true;
        }
        if (!protesto) {
            return 41;
        }
        lock.unlockWrite(buena);
        protesto = false;
        try {
            lock.unlockWrite(buena); // ya soltado
        } catch (IllegalMonitorStateException e) {
            protesto = true;
        }
        if (!protesto) {
            return 42;
        }

        // --- 9. Exclusion real: tres escritores, 100 vueltas cada uno -------------------------
        StampedLock duro = new StampedLock();
        int[] compartido = new int[2];
        StampedWriter e1 = new StampedWriter();
        StampedWriter e2 = new StampedWriter();
        StampedWriter e3 = new StampedWriter();
        e1.lock = duro; e1.compartido = compartido; e1.vueltas = 100;
        e2.lock = duro; e2.compartido = compartido; e2.vueltas = 100;
        e3.lock = duro; e3.compartido = compartido; e3.vueltas = 100;
        e1.start(); e2.start(); e3.start();
        e1.join(); e2.join(); e3.join();
        if (compartido[0] != 300 || compartido[1] != 300) {
            return 43; // un incremento perdido = la exclusion no existe
        }

        // --- 10. El sello optimista con escritores encima -------------------------------------
        // El escritor deja siempre los dos campos iguales; el lector optimista que valida su
        // sello NO puede haber visto un par distinto. Si `validate` mintiera, aca se veria.
        StampedLock mixto = new StampedLock();
        int[] par = new int[2];
        StampedWriter ew = new StampedWriter();
        ew.lock = mixto; ew.compartido = par; ew.vueltas = 200;
        StampedOptReader lector = new StampedOptReader();
        lector.lock = mixto; lector.compartido = par; lector.vueltas = 200;
        ew.start(); lector.start();
        ew.join(); lector.join();
        if (lector.rotas != 0) {
            return 44;
        }
        if (par[0] != 200 || par[1] != 200) {
            return 45;
        }
        // Y la prueba no puede ser vacia: las 200 lecturas optimistas tienen que haber validado.
        if (lector.sinSuerte != 0 || lector.limpias != 200) {
            return 46;
        }

        return -1;
    }
}
