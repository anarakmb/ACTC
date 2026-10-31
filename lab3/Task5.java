import java.io.*;
import java.util.Arrays;

/**
 * Задача 5. Чтение из файла координат точек (x y z) и вычисление геометрического центра
 * (среднее по x, y и z). Две параллельные версии:
 *   A - параллельный цикл (декомпозиция данных): массив делится на блоки, потоки считают частичные суммы
 *       по x, y, z, затем суммы объединяются (редукция);
 *   B - функциональная декомпозиция: три независимые функции, каждая в своём потоке
 *       (поток 1 суммирует x, поток 2 - y, поток 3 - z). Аналог #pragma omp sections.
 * Для сравнения выполняется последовательная версия.
 *
 * Запуск: java Task5 <файл> [потоки=4]   (потоки используются в версии A)
 * Формат файла: на каждой строке три числа через пробел: x y z
 */
public class Task5 {
    static double[] xs, ys, zs;
    static int n;

    static void readFile(String name) throws IOException {
        int cap = 1024;
        xs = new double[cap]; ys = new double[cap]; zs = new double[cap];
        n = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(name), 1 << 16)) {
            String line;
            int lineNo = 0;
            while ((line = r.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split("[\\s;,]+");
                if (p.length < 3) throw new IOException("Line " + lineNo + ": expected 3 numbers");
                if (n == cap) {
                    cap *= 2;
                    xs = Arrays.copyOf(xs, cap); ys = Arrays.copyOf(ys, cap); zs = Arrays.copyOf(zs, cap);
                }
                xs[n] = Double.parseDouble(p[0]);
                ys[n] = Double.parseDouble(p[1]);
                zs[n] = Double.parseDouble(p[2]);
                n++;
            }
        }
    }

    static double sum(double[] a, int from, int to) {
        double s = 0;
        for (int i = from; i < to; i++) s += a[i];
        return s;
    }

    static double[] sequential() {
        return new double[]{sum(xs, 0, n) / n, sum(ys, 0, n) / n, sum(zs, 0, n) / n};
    }

    /** Версия A: параллельный цикл с редукцией. */
    static double[] parallelLoop(int p) throws InterruptedException {
        double[][] part = new double[p][3];     // частичные суммы каждого потока (private-копии)
        Thread[] ts = new Thread[p];
        int base = n / p, rem = n % p, from = 0;
        for (int t = 0; t < p; t++) {
            int to = from + base + (t < rem ? 1 : 0);
            final int id = t, f = from, e = to;
            ts[t] = new Thread(() -> {
                double sx = 0, sy = 0, sz = 0;
                for (int i = f; i < e; i++) { sx += xs[i]; sy += ys[i]; sz += zs[i]; }
                part[id][0] = sx; part[id][1] = sy; part[id][2] = sz;
            });
            ts[t].start();
            from = to;
        }
        for (Thread t : ts) t.join();
        double sx = 0, sy = 0, sz = 0;           // reduction(+)
        for (double[] q : part) { sx += q[0]; sy += q[1]; sz += q[2]; }
        return new double[]{sx / n, sy / n, sz / n};
    }

    /** Версия B: функциональная декомпозиция (sections): три функции в трёх потоках. */
    static double[] functional() throws InterruptedException {
        double[] res = new double[3];
        Thread tx = new Thread(() -> res[0] = sum(xs, 0, n) / n);
        Thread ty = new Thread(() -> res[1] = sum(ys, 0, n) / n);
        Thread tz = new Thread(() -> res[2] = sum(zs, 0, n) / n);
        tx.start(); ty.start(); tz.start();
        tx.join(); ty.join(); tz.join();
        return res;
    }

    static void report(String name, double[] c, double[] ref, double sec) {
        double diff = Math.max(Math.abs(c[0] - ref[0]), Math.max(Math.abs(c[1] - ref[1]), Math.abs(c[2] - ref[2])));
        System.out.printf("%-28s center = (%.6f, %.6f, %.6f)  time %.3f ms  |diff vs seq| = %.2e%n",
                name, c[0], c[1], c[2], sec * 1e3, diff);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.err.println("Usage: java Task5 <file> [threads]"); return; }
        int p = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        if (p < 1) { System.err.println("threads >= 1"); return; }

        long t0 = System.nanoTime();
        readFile(args[0]);
        double readSec = (System.nanoTime() - t0) / 1e9;
        if (n == 0) { System.err.println("File has no points"); return; }
        System.out.printf("Read %d points from %s in %.3f s%n", n, args[0], readSec);

        // прогрев и измерение: берём лучшее из 5 запусков каждой версии
        double[] ref = null, c;
        double bs = 1e9, ba = 1e9, bb = 1e9;
        double[] ca = null, cb = null;
        for (int r = 0; r < 5; r++) {
            t0 = System.nanoTime(); ref = sequential(); bs = Math.min(bs, (System.nanoTime() - t0) / 1e9);
            t0 = System.nanoTime(); ca = parallelLoop(p); ba = Math.min(ba, (System.nanoTime() - t0) / 1e9);
            t0 = System.nanoTime(); cb = functional();    bb = Math.min(bb, (System.nanoTime() - t0) / 1e9);
        }
        report("sequential", ref, ref, bs);
        report("A: parallel loop, " + p + " thr", ca, ref, ba);
        report("B: functional, 3 threads", cb, ref, bb);
    }
}
