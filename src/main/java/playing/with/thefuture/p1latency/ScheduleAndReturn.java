package playing.with.thefuture.p1latency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Schedule some dummy tasks that simulates latency and return as soon as the tasks have been submitted to the service.
 * Here we're not blocking the caller.
 * As soon as the tasks are submitted, we return.
 */
public class ScheduleAndReturn {

    public static void main(final String[] args) throws InterruptedException,
            ExecutionException {
        final int numberOfTasks = 100;

        //create an executor
        final ExecutorService service = Executors.newFixedThreadPool(5);

        //create dummy task
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < numberOfTasks; i++) {
            tasks.add(newCallable());
        }
        //schedule them
        final List<Future<Boolean>> futures = scheduleTasks(tasks, service);

        System.out.println("done scheduling " + futures);
    }

    public static List<Future<Boolean>> scheduleTasks(
            List<Callable<Boolean>> tasks,
            ExecutorService service) {
        return tasks.stream()
                .map((t) -> service.submit(t))
                .collect(Collectors.toList());
    }

    static Callable<Boolean> newCallable() {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Thread.sleep((long) (Math.random() * 1000));
                return true;
            }
        };
    }
}
