import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.openapi.CachingOpenApiSpecRepository;
import io.github.flexksx.openapi.OpenApiSpecReadException;
import io.github.flexksx.openapi.OpenApiSpecReader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class CachingOpenApiSpecRepositoryTest {

  private static final String SPEC_LOCATION_A = "spec-location-a";
  private static final String SPEC_LOCATION_B = "spec-location-b";
  private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);
  private static final Duration ONE_NANOSECOND = Duration.ofNanos(1);
  private static final int THREAD_COUNT = 8;
  private static final int CALLS_PER_THREAD = 50;
  private static final int TIMEOUT_SECONDS = 10;

  private final RecordingSpecReader specReader = new RecordingSpecReader();
  private final TickingClock clock = new TickingClock();
  private final CachingOpenApiSpecRepository repository =
      new CachingOpenApiSpecRepository(specReader, REFRESH_INTERVAL, clock);

  @Test
  void getTwiceWithinTheRefreshInterval_readsTheSpecOnce() throws Exception {
    repository.get(SPEC_LOCATION_A);
    repository.get(SPEC_LOCATION_A);

    assertThat(specReader.readCountOf(SPEC_LOCATION_A)).isEqualTo(1);
  }

  @Test
  void getExactlyAtTheRefreshInterval_readsTheSpecOnce() throws Exception {
    repository.get(SPEC_LOCATION_A);
    clock.advanceBy(REFRESH_INTERVAL);
    repository.get(SPEC_LOCATION_A);

    assertThat(specReader.readCountOf(SPEC_LOCATION_A)).isEqualTo(1);
  }

  @Test
  void getOneNanosecondPastTheRefreshInterval_readsTheSpecAgain() throws Exception {
    repository.get(SPEC_LOCATION_A);
    clock.advanceBy(REFRESH_INTERVAL.plus(ONE_NANOSECOND));
    repository.get(SPEC_LOCATION_A);

    assertThat(specReader.readCountOf(SPEC_LOCATION_A)).isEqualTo(2);
  }

  @Test
  void getWhenTheReaderStartsFailingAfterASuccessfulRead_servesTheCachedSpec() throws Exception {
    OpenAPI firstRead = repository.get(SPEC_LOCATION_A);
    specReader.startFailing();
    clock.advanceBy(REFRESH_INTERVAL.plus(ONE_NANOSECOND));

    assertThat(repository.get(SPEC_LOCATION_A)).isSameAs(firstRead);
  }

  @Test
  void getWhenTheReaderFailsOnTheFirstRead_throwsTheReadFailure() {
    specReader.startFailing();

    assertThatThrownBy(() -> repository.get(SPEC_LOCATION_A))
        .isInstanceOf(OpenApiSpecReadException.class)
        .hasMessageContaining(SPEC_LOCATION_A);
  }

  @Test
  void getAfterAFailedFirstRead_cachesNothingAndReadsAgain() throws Exception {
    specReader.startFailing();
    assertThatThrownBy(() -> repository.get(SPEC_LOCATION_A))
        .isInstanceOf(OpenApiSpecReadException.class);
    specReader.stopFailing();

    assertThat(repository.get(SPEC_LOCATION_A).getInfo().getTitle()).isEqualTo(SPEC_LOCATION_A);
  }

  @Test
  void getFromManyThreads_readsEachSpecLocationOnceAndNeverMixesThem() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
    CountDownLatch startGate = new CountDownLatch(1);

    List<Future<Void>> futures = new ArrayList<>();
    for (Callable<Void> task : createConcurrentGetTasks(startGate)) {
      futures.add(pool.submit(task));
    }
    startGate.countDown();

    try {
      for (Future<Void> future : futures) {
        future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(specReader.readCountOf(SPEC_LOCATION_A)).isEqualTo(1);
    assertThat(specReader.readCountOf(SPEC_LOCATION_B)).isEqualTo(1);
  }

  private List<Callable<Void>> createConcurrentGetTasks(CountDownLatch startGate) {
    List<Callable<Void>> tasks = new ArrayList<>();

    for (int thread = 0; thread < THREAD_COUNT; thread++) {
      String specLocation = (thread % 2 == 0) ? SPEC_LOCATION_A : SPEC_LOCATION_B;
      tasks.add(
          () -> {
            startGate.await();
            for (int call = 0; call < CALLS_PER_THREAD; call++) {
              assertThat(repository.get(specLocation).getInfo().getTitle()).isEqualTo(specLocation);
            }
            return null;
          });
    }
    return tasks;
  }

  private static final class RecordingSpecReader implements OpenApiSpecReader {

    private final Map<String, AtomicInteger> readCountBySpecLocation = new ConcurrentHashMap<>();
    private volatile boolean failing;

    @Override
    public OpenAPI read(String specLocation) throws OpenApiSpecReadException {
      readCountBySpecLocation
          .computeIfAbsent(specLocation, location -> new AtomicInteger())
          .incrementAndGet();
      if (failing) {
        throw new OpenApiSpecReadException(specLocation);
      }
      return new OpenAPI().info(new Info().title(specLocation));
    }

    void startFailing() {
      failing = true;
    }

    void stopFailing() {
      failing = false;
    }

    int readCountOf(String specLocation) {
      return readCountBySpecLocation.getOrDefault(specLocation, new AtomicInteger()).get();
    }
  }

  private static final class TickingClock extends Clock {

    private Instant instant = Instant.EPOCH;

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }

    void advanceBy(Duration amount) {
      instant = instant.plus(amount);
    }
  }
}
