package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object delegate;
  private final ProfilingState state;
  private final ZonedDateTime startTime;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock,Object delegate,ProfilingState state,ZonedDateTime startTime) {
    this.clock = Objects.requireNonNull(clock);
    this.delegate = delegate;
    this.state = state;
    this.startTime = startTime;

  }


  // to test -   mvn test -Dtest=ProfilerImplTest

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable{
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.

    Instant startTime = null;

    Object objInvoking;

    boolean checkProfiled = method.getAnnotation(Profiled.class) != null;
    if(checkProfiled){  // record start time for profiled methods
      startTime = clock.instant();
    }
// to test -   mvn test -Dtest=ProfilerImplTest
    try{
      objInvoking = method.invoke(this.delegate, args);   //invoking the method
    }catch(InvocationTargetException ex){
      throw ex.getTargetException();
    }
    catch(IllegalAccessException e){
      throw new RuntimeException(e);
    }finally {   //recording time taken by method in finally
      if (checkProfiled) {
        Duration totalTime = Duration.between(startTime, clock.instant());
        state.record(delegate.getClass(), method, totalTime);
      }
    }

    return objInvoking;
  }
}
