import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Задача 2. Массив a[i] = i, результат b[i] = (a[i-1] + a[i] + a[i+1]) / 3.0 для 1..n-2.
 * Крайние элементы b[0] и b[n-1] не вычисляются (остаются 0).
 * Итерации распределяются между потоками вручную, повторяя типы schedule из OpenMP:
 *   static            - по одному непрерывному блоку на поток
 *   static,chunk      - блоки по chunk итераций циклически (round-robin)
 *   dynamic,chunk     - поток забирает очередной блок, когда освободился
 *   guided,chunk      - размер блока = max(chunk, осталось/потоков), уменьшается
 *
 * Запуск: java Task2 [потоки=8] [размер=16000] [schedule=all|static|dynamic|guided] [chunk=100] [heavy=0|1]
 *   heavy=1 добавляет к итерации нагрузку, растущую с номером (неравномерные итерации).
 *   Для static без указания chunk - один блок на поток.
 */
public class Task2 {
    static int threads, size, chunk;
    static boolean heavy;
    static double[] a, b;
    static final double[] sink = new double[1024]; // чтобы JIT не выбросил искусственную нагрузку

    static void compute(int i, int tid) {
        b[i] = (a[i - 1] + a[i] + a[i + 1]) / 3.0;
        if (heavy) {
            double s = 0;
            int k = i / 20;                      // чем больше i, тем тяжелее итерация
            for (int j = 0; j < k; j++) s += Math.sqrt(j + i);
            sink[tid] += s;
        }
    }

    /** Итерация цикла с номером p соответствует элементу i = p + 1 (p = 0..size-3). */
    static void runSchedule(String type, int ch) throws InterruptedException {
        Arrays.fill(b, 0);
        final int m = size - 2;
        final long[] count = new long[threads];
        final AtomicInteger next = new AtomicInteger(0);
        final Object lock = new Object();
        final int[] guidedPos = {0};
        Thread[] ts = new Thread[threads];

        for (int t = 0; t < threads; t++) {
            final int tid = t;
            ts[t] = new Thread(() -> {
                switch (type) {
                    case "static" -> {
                        if (ch <= 0) {                           // один блок на поток
                            int base = m / threads, rem = m % threads;
                            int from = tid * base + Math.min(tid, rem);
                            int to = from + base + (tid < rem ? 1 : 0);
                            for (int p = from; p < to; p++) { compute(p + 1, tid); count[tid]++; }
                        } else {                                  // блоки по ch циклически
                            for (int s = tid * ch; s < m; s += threads * ch)
                                for (int p = s; p < Math.min(s + ch, m); p++) { compute(p + 1, tid); count[tid]++; }
                        }
                    }
                    case "dynamic" -> {
                        int s;
                        while ((s = next.getAndAdd(ch)) < m)
                            for (int p = s; p < Math.min(s + ch, m); p++) { compute(p + 1, tid); count[tid]++; }
                    }
                    case "guided" -> {
                        while (true) {
                            int s, e;
                            synchronized (lock) {
                                s = guidedPos[0];
                                if (s >= m) break;
                                int blk = Math.max(ch, (m - s) / threads);
                                e = Math.min(m, s + blk);
                                guidedPos[0] = e;
                            }
                            for (int p = s; p < e; p++) { compute(p + 1, tid); count[tid]++; }
                        }
                    }
                    default -> throw new IllegalArgumentException(type);
                }
            });
        }
        long t0 = System.nanoTime();
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        long dt = System.nanoTime() - t0;

        // проверка последовательной версией
        double[] ref = new double[size];
        for (int i = 1; i < size - 1; i++) ref[i] = (a[i - 1] + a[i] + a[i + 1]) / 3.0;
        boolean ok = Arrays.equals(ref, b);

        String name = type + (type.equals("static") && ch <= 0 ? "" : "," + ch);
        System.out.printf("schedule(%s): time %.3f ms, result %s%n", name, dt / 1e6, ok ? "OK" : "ERROR");
        System.out.println("  iterations per thread: " + Arrays.toString(count));
        System.out.printf("  b[1]=%.1f b[2]=%.1f b[%d]=%.1f b[%d]=%.1f%n", b[1], b[2], size - 2, b[size - 2], size - 1, b[size - 1]);
    }

    public static void main(String[] args) throws InterruptedException {
        threads = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        size = args.length > 1 ? Integer.parseInt(args[1]) : 16000;
        String sched = args.length > 2 ? args[2] : "all";
        chunk = args.length > 3 ? Integer.parseInt(args[3]) : 100;
        heavy = args.length > 4 && args[4].equals("1");
        if (threads < 1 || size < 3) { System.err.println("потоки >= 1, размер >= 3"); return; }

        a = new double[size];
        b = new double[size];
        for (int i = 0; i < size; i++) a[i] = i;
        System.out.printf("Threads: %d, array size: %d, chunk: %d, uneven load: %s%n", threads, size, chunk, heavy);

        if (sched.equals("all")) {
            runSchedule("static", 0);
            runSchedule("static", chunk);
            runSchedule("dynamic", chunk);
            runSchedule("guided", chunk);
        } else if (sched.equals("static")) {
            runSchedule("static", args.length > 3 ? chunk : 0);
        } else {
            runSchedule(sched, chunk);
        }
    }
}
