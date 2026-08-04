import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import io.github.flexksx.openapi.SwaggerSpecRepository;

public class SwaggerSpecRepositoryTest {
  String SPEC_NAME_JSON = "JSON";
  String SPEC_NAME_YAML = "YAML";
  String SPEC_NAME = "openapi-specs/sample-rest-api-client.openapi";
  String SPEC_JSON = SPEC_NAME + ".json";
  String SPEC_YAML = SPEC_NAME + ".yaml";

  @Test
  public void concurrentSetLocationAndGetOpenApi_doesNotProduceCorruptedState() throws Exception {
    int THREAD_COUNT = 8;
    int TIMEOUT_SECONDS = 5;

    ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
    SwaggerSpecRepository repository = new SwaggerSpecRepository(SPEC_JSON, Duration.ofSeconds(60));

    CountDownLatch startGate = new CountDownLatch(1);
    Queue<String> observedTitles = new ConcurrentLinkedQueue<>();
    List<Callable<Void>> tasks = createTasks(THREAD_COUNT, startGate, repository, observedTitles);

    List<Future<Void>> futures = new ArrayList<>();
    for (Callable<Void> task : tasks) {
      futures.add(pool.submit(task));
    }
    startGate.countDown();

    for (Future<Void> future : futures) {
      future.get(5, TimeUnit.SECONDS);
    }
    pool.shutdown();
    pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);

    assertThat(observedTitles).isNotEmpty();
    assertThat(observedTitles)
        .allMatch(title -> title.equals(SPEC_NAME_JSON) || title.equals(SPEC_NAME_YAML));
    System.out.println(
        observedTitles.size()
            + " observations, distinct: "
            + observedTitles.stream().distinct().toList());
  }

  private @NonNull List<Callable<Void>> createTasks(
      int THREAD_COUNT,
      CountDownLatch startGate,
      SwaggerSpecRepository repository,
      Queue<String> observedTitles) {
    List<Callable<Void>> tasks = new ArrayList<>();

    for (int i = 0; i < THREAD_COUNT / 2; i++) {
      String targetSpec = (i % 2 == 0) ? SPEC_JSON : SPEC_YAML;
      tasks.add(
          () -> {
            startGate.await();
            repository.setSpecLocation(targetSpec);
            return null;
          });
    }

    for (int i = 0; i < THREAD_COUNT / 2; i++) {
      tasks.add(
          () -> {
            startGate.await();
            long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
            while (System.nanoTime() < deadline) {
              observedTitles.add(repository.getOpenApi().getInfo().getTitle());
            }
            return null;
          });
    }
    return tasks;
  }
}
