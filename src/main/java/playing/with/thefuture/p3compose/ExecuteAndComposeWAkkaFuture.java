package playing.with.thefuture.p3compose;

import static akka.dispatch.Futures.future;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.dispatch.ExecutionContexts;
import akka.dispatch.Mapper;
import akka.util.Timeout;

import com.google.common.collect.Lists;

/**
 * Now, let's see how we can add some extra logic without blocking. Akka future are more composable,
 * and we can pipe operations to it. Here we just try to negate the values and filter the odd ones
 * It's good for single future but not so good when we're dealing with collection of futures. When
 * we apply any transformation we need to do something like futures.map(f -> f.map(someFunction))
 * instead of simply having futures.map(someFunction)
 */
public class ExecuteAndComposeWAkkaFuture {

    public static void main(final String[] args) throws Exception {
        final int numberOfTasks = 10;
        ExecutorService service = Executors.newFixedThreadPool(5);
        ExecutionContext execContext = ExecutionContexts
            .fromExecutorService(service);

        // populate with dummy tasks
        // here, a task has a change to fail
        Queue<Callable<Integer>> tasks = new LinkedList<>();
        for (int i = 0; i < numberOfTasks; i++) {
            tasks.add(() -> {
                return new Random().nextInt(2000);
            });
        }

        List<Integer> is = Lists.newArrayList(1, 2, 3);
        List<Integer> isPlus1 = is.stream().map(i -> i + 1)
            .collect(Collectors.toList());
        System.out.println(isPlus1);

        List<Future<Integer>> futures = executeTask(execContext, tasks);
        futures.stream().map(f -> f.map(new Mapper<Integer, Integer>() {
            @Override
            public Integer apply(Integer i) {
                return i + 1;
            }
        }, execContext));

        List<Future<Integer>> asNegVals = futures.stream()
            .map(f -> f.map(new Mapper<Integer, Integer>() {
                @Override
                public Integer apply(Integer i) {
                    return -i;
                }
            }, execContext)).collect(Collectors.toList());

        Timeout timeout = new Timeout(Duration.create(5, "seconds"));
        for (Future<Integer> f: asNegVals) {
            System.out.println(Await.result(f, timeout.duration()));
        }
        service.shutdown();
    }

    public static List<Future<Integer>>
        executeTask(final ExecutionContext execContext,
            final Queue<Callable<Integer>> tasks) {

        return tasks.stream().map((t) -> future(t, execContext))
            .collect(Collectors.toList());
    }

}
