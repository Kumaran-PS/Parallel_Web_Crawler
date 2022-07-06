package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate)throws IllegalArgumentException {
    Objects.requireNonNull(klass);

    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.

//    For reference :
//    boolean isProfiled2 = false;
//    for (Method method : klass.getMethods()) {
//      if (method.isAnnotationPresent(Profiled.class) == null) {
//      } else {
//        isProfiled2 = true;
//        break;
//      }
//    }
    boolean isProfiled = false;

    List<Method> methodsKlass = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
    if (methodsKlass.isEmpty()) isProfiled = false;
    if(methodsKlass.stream().anyMatch(x -> x.getAnnotation(Profiled.class) != null)){
      isProfiled = true;
    }
    if (isProfiled == false) {
      throw new IllegalArgumentException(klass.getName() + " - does not contain profiled methods");
    }
    Object proxy;
    proxy = Proxy.newProxyInstance(
            ProfilerImpl.class.getClassLoader(),
            new Class[]{klass},
            new ProfilingMethodInterceptor(clock, delegate, state, startTime));
    return (T) proxy;                   // returning a dynamic proxy from this method

  }


// to test -   mvn test -Dtest=ProfilerImplTest

  @Override
  public void writeData(Path path) throws IOException{
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.
    try(Writer writer = Files.newBufferedWriter(path)) {
      if(Files.notExists(path)){            // checking if a file existing at a path
        Files.createFile(path);             // if not present it is newly created
      }
      writeData(writer);
      writer.flush();
    } catch (IOException e) {
      System.out.println("Error in profiler implementation " +e);
    }

  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
