/**
 * Задача 1. Каждый поток печатает свой идентификатор, общее число потоков и "Hello World".
 */
public class Task1 {
    public static void main(String[] args) throws InterruptedException {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        if (n < 1) { System.err.println("Число потоков должно быть >= 1"); return; }

        Thread[] threads = new Thread[n];
        for (int i = 0; i < n; i++) {
            final int id = i;
            threads[i] = new Thread(() ->
                    System.out.println("Thread " + id + " of " + n + ": Hello World"));
        }
        for (Thread t : threads) t.start();   // fork
        for (Thread t : threads) t.join();    // join
    }
}
