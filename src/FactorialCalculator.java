import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FactorialCalculator {
//    Function for calculating the factorial
    private static long factorial(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    public static void main(String[] args) {
        // The size of the thread pool is set by the user via the console
        Scanner scanner = new Scanner(System.in);
        System.out.print("Введите размер пула потоков: ");
        int poolSize = scanner.nextInt();

        // Queue for storing numbers from input.txt file
        BlockingQueue<Integer> inputQueue = new LinkedBlockingQueue<>();
        // Queue for storing results in the order in which they are received
        BlockingQueue<String> outputQueue = new LinkedBlockingQueue<>();
        // Limit for processing no more than 100 numbers in 1 second
        AtomicInteger limitCounter = new AtomicInteger(0);

        // Read stream from input.txt file
        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new FileReader("input.txt"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim(); //Clean up the gaps around the edges
                    if (!line.isEmpty()) {  //Check that the string is not empty
                        try {
                            int number = Integer.parseInt(line); // Convert the string to a number
                            inputQueue.put(number);  //Adding numbers to the queue
                        } catch (NumberFormatException ignored) {
                            // Skip invalid lines
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Write stream to output.txt file
        Thread writerThread = new Thread(() -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
                while (true) {
                    String result = outputQueue.poll(1, TimeUnit.SECONDS);  // Get the result with timeout
                    if (result != null) {
                        writer.write(result);
                        writer.newLine();
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Thread pool for calculating factorials
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        // Scheduler to reset the counter every 1 second
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> limitCounter.set(0), 0, 1, TimeUnit.SECONDS);

        // A thread to execute tasks from inputQueue
        Thread calculatorThread = new Thread(() -> {
            try {
                while (true) {
                    int number = inputQueue.take(); // Retrieve the number for processing

                    // Check the limit of 100 calculations per second
                    while (limitCounter.get() >= 100) {
                        Thread.sleep(10); // Wait for the limit to be reset
                    }

                    // Увеличиваем счетчик лимита
                    limitCounter.incrementAndGet();

                    // Start calculating the factorial in the thread pool
                    executor.submit(() -> {
                        long result = factorial(number);
                        try {
                            outputQueue.put(number + " = " + result); // Result to write queue
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Starting threads
        readerThread.start();
        writerThread.start();
        calculatorThread.start();

        // Waiting for threads to complete
        try {
            readerThread.join();
            calculatorThread.join();
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            writerThread.join();
            scheduler.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Программа завершена.");
    }
}
