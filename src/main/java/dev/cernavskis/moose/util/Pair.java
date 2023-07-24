package dev.cernavskis.moose.util;

public record Pair<L, R>(L first, R second) {
  public static <L, R> Pair<L, R> of(L first, R second) {
    return new Pair<>(first, second);
  }

  @Override
  public String toString() {
    return String.format("(%s, %s)", first, second);
  }
}
