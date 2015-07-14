package playing.with.thefuture.p1latency;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Execute some dummy tasks that simulates latency and block until they complete.
 * Here we use the invokeAll methods which will block until all tasks have actually been executed.
 */
public class ExecuteAndBlock {

    public static void main(final String[] args) throws InterruptedException {
        final int numberOfTasks = 100;
        final int numberOfThreads = 5;
        final ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);

        //create some dummy tasks
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < numberOfTasks; i++) {
            tasks.add(newCallable());
        }
        //execute
        System.out.println("executed tasks: " + executeTasks(service, tasks));
        service.shutdown();
    }

    public static List<Future<Boolean>> executeTasks(
            final ExecutorService service,
            final List<Callable<Boolean>> tasks)
            throws InterruptedException {
        return service.invokeAll(tasks);
    }

    static Callable<Boolean> newCallable() {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Thread.sleep((long) (Math.random() * 1000));
                return new Random().nextBoolean();
            }
        };
    }
}
