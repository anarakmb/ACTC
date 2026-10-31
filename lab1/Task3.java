import java.util.concurrent.*;

/**
 * Задача 3. Потоки печатают свои идентификаторы в ОБРАТНОМ порядке (n-1, n-2, ..., 0).
 * Запуск: java Task3 [потоки=8] [способ=all|1..6]
 *  1 - запуск и join по цепочке (поток n-1 стартует первым и завершается до старта следующего)
 *  2 - монитор: общая переменная turn + wait/notifyAll
 *  3 - семафоры: поток i ждёт свой семафор, после печати открывает семафор потока i-1
 *  4 - CountDownLatch: поток i ждёт, пока напечатает поток i+1
 *  5 - CyclicBarrier: n фаз, в фазе k печатает поток n-1-k (аналог omp barrier)
 *  6 - потоки пишут строки в общий массив, main выводит массив с конца
 */
public class Task3 {
    static String msg(int id, int n) { return "Thread " + id + " of " + n + ": Hello World"; }

    static void method1(int n) throws Exception {
        for (int id = n - 1; id >= 0; id--) {
            final int fid = id;
            Thread t = new Thread(() -> System.out.println(msg(fid, n)));
            t.start();
            t.join();
        }
    }

    static void method2(int n) throws Exception {
        final Object lock = new Object();
        final int[] turn = {n - 1};
        Thread[] ts = new Thread[n];
        for (int i = 0; i < n; i++) {
            final int id = i;
            ts[i] = new Thread(() -> {
                synchronized (lock) {
                    try {
                        while (turn[0] != id) lock.wait();
                    } catch (InterruptedException e) { return; }
                    System.out.println(msg(id, n));
                    turn[0]--;
                    lock.notifyAll();
                }
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
    }

    static void method3(int n) throws Exception {
        Semaphore[] sem = new Semaphore[n];
        for (int i = 0; i < n; i++) sem[i] = new Semaphore(i == n - 1 ? 1 : 0);
        Thread[] ts = new Thread[n];
        for (int i = 0; i < n; i++) {
            final int id = i;
            ts[i] = new Thread(() -> {
                try {
                    sem[id].acquire();
                    System.out.println(msg(id, n));
                    if (id > 0) sem[id - 1].release();
                } catch (InterruptedException ignored) { }
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
    }

    static void method4(int n) throws Exception {
        CountDownLatch[] done = new CountDownLatch[n];
        for (int i = 0; i < n; i++) done[i] = new CountDownLatch(1);
        Thread[] ts = new Thread[n];
        for (int i = 0; i < n; i++) {
            final int id = i;
            ts[i] = new Thread(() -> {
                try {
                    if (id < n - 1) done[id + 1].await();
                    System.out.println(msg(id, n));
                    done[id].countDown();
                } catch (InterruptedException ignored) { }
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
    }

    static void method5(int n) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(n);
        Thread[] ts = new Thread[n];
        for (int i = 0; i < n; i++) {
            final int id = i;
            ts[i] = new Thread(() -> {
                try {
                    for (int phase = 0; phase < n; phase++) {
                        if (phase == n - 1 - id) System.out.println(msg(id, n));
                        barrier.await();      // все ждут, пока очередной поток напечатает
                    }
                } catch (InterruptedException | BrokenBarrierException ignored) { }
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
    }

    static void method6(int n) throws Exception {
        String[] res = new String[n];
        Thread[] ts = new Thread[n];
        for (int i = 0; i < n; i++) {
            final int id = i;
            ts[i] = new Thread(() -> res[id] = msg(id, n));
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        for (int i = n - 1; i >= 0; i--) System.out.println(res[i]);
    }

    public static void main(String[] args) throws Exception {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        String which = args.length > 1 ? args[1] : "all";
        if (n < 1) { System.err.println("Число потоков должно быть >= 1"); return; }
        for (int m = 1; m <= 6; m++) {
            if (!which.equals("all") && !which.equals(String.valueOf(m))) continue;
            System.out.println("--- Method " + m + " ---");
            switch (m) {
                case 1 -> method1(n);
                case 2 -> method2(n);
                case 3 -> method3(n);
                case 4 -> method4(n);
                case 5 -> method5(n);
                case 6 -> method6(n);
            }
        }
    }
}
