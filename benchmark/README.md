Using [Java Microbenchmark Harness](https://github.com/openjdk/jmh) (JMH) to for analysing performance. Benchmarks were initialized with the JMH kotlin maven archetype.

Make sure to build and install the whole project first:
```shell
# Run from sources root
mvn clean install
```

Then, to run benchmarks, run:
```shell
java -jar ./benchmark/target/benchmarks.jar
```