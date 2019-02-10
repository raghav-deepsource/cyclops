package cyclops.reactiveSeq;

import cyclops.reactive.ReactiveSeq;
import cyclops.reactive.Spouts;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class OnEmptySwitch {

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(
        iterations = 10
    )
    @Measurement(
        iterations = 10
    )
    @Fork(1)
    public void onEmptySwitchNew(Blackhole bh) {
        for (int k = 0; k < 100; k++) {
            bh.consume(Spouts.of()
                .onEmptySwitch2(() -> Spouts.of(1, 2, 3))
                .toList());
        }
    }


    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(
        iterations = 10
    )
    @Measurement(
        iterations = 10
    )
    @Fork(1)
    public void onEmptySwitchOld(Blackhole bh) {
        for (int k = 0; k < 100; k++) {
            bh.consume(Spouts.of()
                    .onEmptySwitch(() -> Spouts.of(1, 2, 3))
                    .toList());
        }
    }



}
