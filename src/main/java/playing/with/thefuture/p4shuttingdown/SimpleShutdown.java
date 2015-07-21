package playing.with.thefuture.p4shuttingdown;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Here we want to make sure that even if we already returned our results,
 * We'll still shutdown our executor on completion and the shutdown action is not blocking our client.
 * In fact, we want to hide as much as possible to caller.
 * We don't want the caller to have any knowledge about how those tasks are being scheduled
 * And we don't him to have to shutdown the service nor that we want him to create the service.
 * The simpler solution is just to put the shutdown call inside a finally statement.
 */
public class SimpleShutdown {

    public static void main(String[] args) {
        final int numberOfTasks = 10;
        CountDownLatch latch = new CountDownLatch(numberOfTasks);
        
        //create some dummy tasks
        final Queue<Callable<Boolean>> tasks = new LinkedList<>();
        for (int i = 0; i < numberOfTasks; i++) {
            tasks.add(newCallable(latch));
        }
        //execute
        System.out.println("executed tasks: " + executeTasks(tasks));
        System.out.println("hand on there");
    }

    static List<Future<Boolean>> executeTasks(
            final Queue<Callable<Boolean>> tasks) {

        final ExecutorService service = Executors.newFixedThreadPool(5);
        final List<Future<Boolean>> results = new ArrayList<>();
        try {
            while (!tasks.isEmpty()) {
                Callable<Boolean> task = tasks.poll();
                results.add(service.submit(task));
            }
        } finally {
            service.shutdown();
        }
        return results;
    }

    static Callable<Boolean> newCallable(CountDownLatch latch) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Thread.sleep(3000);
                latch.countDown();
                return true;
            }
        };
    }
}
