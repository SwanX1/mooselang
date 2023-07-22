package dev.cernavskis.moose.interpreter.types;

@FunctionalInterface
public interface RuntimeCallable {
  RuntimeType<?> call(RuntimeType<?>[] args);
}
