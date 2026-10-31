import java.util.Random;

/**
 * Задача 4. Перемножение больших матриц C = A * B, сравнение последовательной и параллельной версий.
 * Параллельная версия: строки результата делятся на непрерывные блоки (аналог schedule(static)),
 * каждый поток считает свой блок строк. Готовые функции умножения матриц не используются.
 *
 * Запуск: java Task4 [размер=800] [потоки=2,4,8] [повторов=3]
 *   потоки - список через запятую, например 2,4,8
 */
public class Task4 {

    /** Умножение строк [from, to) результата. B транспонирована -> оба внутренних обращения идут по строкам. */
    static void mulRows(double[][] a, double[][] bt, double[][] c, int n, int from, int to) {
        for (int i = from; i < to; i++) {
            double[] ai = a[i];
            double[] ci = c[i];
            for (int j = 0; j < n; j++) {
                double[] bj = bt[j];
                double s = 0;
                for (int k = 0; k < n; k++) s += ai[k] * bj[k];
                ci[j] = s;
            }
        }
    }

    static double[][] transpose(double[][] m, int n) {
        double[][] t = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) t[j][i] = m[i][j];
        return t;
    }

    static double[][] multiplySeq(double[][] a, double[][] b, int n) {
        double[][] c = new double[n][n];
        mulRows(a, transpose(b, n), c, n, 0, n);
        return c;
    }

    static double[][] multiplyPar(double[][] a, double[][] b, int n, int p) throws InterruptedException {
        double[][] c = new double[n][n];
        double[][] bt = transpose(b, n);
        Thread[] ts = new Thread[p];
        int base = n / p, rem = n % p;
        int from = 0;
        for (int t = 0; t < p; t++) {
            int to = from + base + (t < rem ? 1 : 0);
            final int f = from, e = to;
            ts[t] = new Thread(() -> mulRows(a, bt, c, n, f, e));
            ts[t].start();
            from = to;
        }
        for (Thread t : ts) t.join();
        return c;
    }

    static double maxDiff(double[][] x, double[][] y, int n) {
        double d = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) d = Math.max(d, Math.abs(x[i][j] - y[i][j]));
        return d;
    }

    public static void main(String[] args) throws InterruptedException {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 800;
        String[] plist = (args.length > 1 ? args[1] : "2,4,8").split(",");
        int reps = args.length > 2 ? Integer.parseInt(args[2]) : 3;
        if (n < 1 || reps < 1) { System.err.println("размер >= 1, повторов >= 1"); return; }

        Random rnd = new Random(42);
        double[][] a = new double[n][n], b = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) { a[i][j] = rnd.nextDouble(); b[i][j] = rnd.nextDouble(); }

        System.out.printf("Matrix size: %d x %d, repetitions: %d, available cores: %d%n",
                n, n, reps, Runtime.getRuntime().availableProcessors());

        // прогрев JIT-компилятора на небольшой матрице
        int w = Math.min(n, 150);
        double[][] wa = new double[w][w], wb = new double[w][w];
        for (int i = 0; i < w; i++) for (int j = 0; j < w; j++) { wa[i][j] = i + j; wb[i][j] = i - j; }
        for (int r = 0; r < 5; r++) { multiplySeq(wa, wb, w); multiplyPar(wa, wb, w, 2); }

        double bestSeq = Double.MAX_VALUE;
        double[][] ref = null;
        for (int r = 0; r < reps; r++) {
            long t0 = System.nanoTime();
            ref = multiplySeq(a, b, n);
            bestSeq = Math.min(bestSeq, (System.nanoTime() - t0) / 1e9);
        }
        System.out.printf("%-12s %10s %10s %12s %s%n", "version", "time, s", "speedup", "efficiency", "check");
        System.out.printf("%-12s %10.3f %10s %12s %s%n", "sequential", bestSeq, "1.00", "-", "-");

        for (String ps : plist) {
            int p = Integer.parseInt(ps.trim());
            double best = Double.MAX_VALUE;
            double[][] c = null;
            for (int r = 0; r < reps; r++) {
                long t0 = System.nanoTime();
                c = multiplyPar(a, b, n, p);
                best = Math.min(best, (System.nanoTime() - t0) / 1e9);
            }
            double speedup = bestSeq / best;
            boolean ok = maxDiff(ref, c, n) == 0.0;
            System.out.printf("%-12s %10.3f %10.2f %11.0f%% %s%n", p + " threads", best, speedup, 100.0 * speedup / p, ok ? "OK" : "MISMATCH");
        }
        System.out.println("(time = best of " + reps + " runs)");
    }
}
