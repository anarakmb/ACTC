import java.io.*;
import java.util.Random;

/**
 * Генератор тестового файла с точками (x y z), по одной точке на строку.
 * Запуск: java GenPoints <число_точек> <файл> [seed=1]
 */
public class GenPoints {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) { System.err.println("Usage: java GenPoints <N> <file> [seed]"); return; }
        long n = Long.parseLong(args[0]);
        long seed = args.length > 2 ? Long.parseLong(args[2]) : 1;
        Random rnd = new Random(seed);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(args[1]), 1 << 16)) {
            StringBuilder sb = new StringBuilder();
            for (long i = 0; i < n; i++) {
                sb.setLength(0);
                for (int k = 0; k < 3; k++) {
                    double v = Math.round((rnd.nextDouble() * 200 - 100) * 1e6) / 1e6;
                    if (k > 0) sb.append(' ');
                    sb.append(v);
                }
                sb.append('\n');
                w.write(sb.toString());
            }
        }
        System.out.println("Written " + n + " points to " + args[1]);
    }
}
