package latte.jdk21.java.util;

import liquidjava.specification.*;
import specification.*;

/**
 * Primitive arrays only: whole/range fill and sequential sort write array slots; copyOf and
 * copyOfRange allocate fresh arrays. Comparisons, hashing, mismatch and binary search preserve
 * inputs. Range copies permit zero padding. No sortedness or exact element-content postcondition is
 * claimed; object/comparator/callback overloads retain conservative effects. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/Arrays.java
 */
@ExternalRefinementsFor("java.util.Arrays")
public interface ArraysSpec {
  @Modifies("array[*]")
  void fill(@Borrowed @Refinement("_ != null") byte[] array, byte value);

  @Modifies({})
  @Refinement("_ != null && length(_) == newLength")
  @Free
  byte[] copyOf(
      @Borrowed @Refinement("_ != null") byte[] array, @Refinement("_ >= 0") int newLength);

  @Modifies("array[*]")
  void sort(@Borrowed @Refinement("_ != null") byte[] array);

  @Modifies({})
  @Refinement("_ >= -length(array) - 1 && _ < length(array)")
  int binarySearch(@Borrowed @Refinement("_ != null") byte[] array, byte key);

  @Modifies("array[*]")
  void fill(@Borrowed @Refinement("_ != null") short[] array, short value);

  @Modifies({})
  @Refinement("_ != null && length(_) == newLength")
  @Free
  short[] copyOf(
      @Borrowed @Refinement("_ != null") short[] array, @Refinement("_ >= 0") int newLength);

  @Modifies("array[*]")
  void sort(@Borrowed @Refinement("_ != null") short[] array);

  @Modifies({})
  @Refinement("_ >= -length(array) - 1 && _ < length(array)")
  int binarySearch(@Borrowed @Refinement("_ != null") short[] array, short key);

  @Modifies("array[*]")
  void fill(@Borrowed @Refinement("_ != null") int[] array, int value);

  @Modifies({})
  @Refinement("_ != null && length(_) == newLength")
  @Free
  int[] copyOf(@Borrowed @Refinement("_ != null") int[] array, @Refinement("_ >= 0") int newLength);

  @Modifies("array[*]")
  void sort(@Borrowed @Refinement("_ != null") int[] array);

  @Modifies({})
  @Refinement("_ >= -length(array) - 1 && _ < length(array)")
  int binarySearch(@Borrowed @Refinement("_ != null") int[] array, int key);

  @Modifies("array[*]")
  void fill(@Borrowed @Refinement("_ != null") long[] array, long value);

  @Modifies({})
  @Refinement("_ != null && length(_) == newLength")
  @Free
  long[] copyOf(
      @Borrowed @Refinement("_ != null") long[] array, @Refinement("_ >= 0") int newLength);

  @Modifies("array[*]")
  void sort(@Borrowed @Refinement("_ != null") long[] array);

  @Modifies({})
  @Refinement("_ >= -length(array) - 1 && _ < length(array)")
  int binarySearch(@Borrowed @Refinement("_ != null") long[] array, long key);

  @Modifies("array[*]")
  void fill(@Borrowed @Refinement("_ != null") char[] array, char value);

  @Modifies({})
  @Refinement("_ != null && length(_) == newLength")
  @Free
  char[] copyOf(
      @Borrowed @Refinement("_ != null") char[] array, @Refinement("_ >= 0") int newLength);

  @Modifies("array[*]")
  void sort(@Borrowed @Refinement("_ != null") char[] array);

  @Modifies({})
  @Refinement("_ >= -length(array) - 1 && _ < length(array)")
  int binarySearch(@Borrowed @Refinement("_ != null") char[] array, char key);

  @Modifies("array[*]")
  void fill(@Borrowed @Refinement("_ != null") float[] array, float value);

  @Modifies({})
  @Refinement("_ != null && length(_) == newLength")
  @Free
  float[] copyOf(
      @Borrowed @Refinement("_ != null") float[] array, @Refinement("_ >= 0") int newLength);

  @Modifies("array[*]")
  void sort(@Borrowed @Refinement("_ != null") float[] array);

  @Modifies({})
  @Refinement("_ >= -length(array) - 1 && _ < length(array)")
  int binarySearch(@Borrowed @Refinement("_ != null") float[] array, float key);

  @Modifies("array[*]")
  void fill(@Borrowed @Refinement("_ != null") double[] array, double value);

  @Modifies({})
  @Refinement("_ != null && length(_) == newLength")
  @Free
  double[] copyOf(
      @Borrowed @Refinement("_ != null") double[] array, @Refinement("_ >= 0") int newLength);

  @Modifies("array[*]")
  void sort(@Borrowed @Refinement("_ != null") double[] array);

  @Modifies({})
  @Refinement("_ >= -length(array) - 1 && _ < length(array)")
  int binarySearch(@Borrowed @Refinement("_ != null") double[] array, double key);

  @Modifies("array[*]")
  void fill(@Borrowed @Refinement("_ != null") boolean[] array, boolean value);

  @Modifies({})
  @Refinement("_ != null && length(_) == newLength")
  @Free
  boolean[] copyOf(
      @Borrowed @Refinement("_ != null") boolean[] array, @Refinement("_ >= 0") int newLength);

  @Modifies({})
  @Free
  @Refinement("_ != null && length(_) == to - from")
  boolean[] copyOfRange(
      @Borrowed @Refinement("_ != null") boolean[] array,
      @Refinement("_ >= 0 && _ <= length(array)") int from,
      @Refinement("_ >= from") int to);

  @Modifies("array[*]")
  void fill(
      @Borrowed @Refinement("_ != null") boolean[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      boolean value);

  @Modifies({})
  boolean equals(@Borrowed boolean[] first, @Borrowed boolean[] second);

  @Modifies({})
  int hashCode(@Borrowed boolean[] array);

  @Modifies({})
  int compare(@Borrowed boolean[] first, @Borrowed boolean[] second);

  @Modifies({})
  @Refinement("_ >= -1 && _ <= length(first) && _ <= length(second)")
  int mismatch(
      @Borrowed @Refinement("_ != null") boolean[] first,
      @Borrowed @Refinement("_ != null") boolean[] second);

  @Modifies({})
  @Free
  @Refinement("_ != null && length(_) == to - from")
  byte[] copyOfRange(
      @Borrowed @Refinement("_ != null") byte[] array,
      @Refinement("_ >= 0 && _ <= length(array)") int from,
      @Refinement("_ >= from") int to);

  @Modifies("array[*]")
  void fill(
      @Borrowed @Refinement("_ != null") byte[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      byte value);

  @Modifies({})
  boolean equals(@Borrowed byte[] first, @Borrowed byte[] second);

  @Modifies({})
  int hashCode(@Borrowed byte[] array);

  @Modifies({})
  int compare(@Borrowed byte[] first, @Borrowed byte[] second);

  @Modifies({})
  @Refinement("_ >= -1 && _ <= length(first) && _ <= length(second)")
  int mismatch(
      @Borrowed @Refinement("_ != null") byte[] first,
      @Borrowed @Refinement("_ != null") byte[] second);

  @Modifies("array[*]")
  void sort(
      @Borrowed @Refinement("_ != null") byte[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to);

  @Modifies({})
  @Refinement("(_ >= from && _ < to) || (_ >= -to - 1 && _ <= -from - 1)")
  int binarySearch(
      @Borrowed @Refinement("_ != null") byte[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      byte key);

  @Modifies({})
  @Free
  @Refinement("_ != null && length(_) == to - from")
  short[] copyOfRange(
      @Borrowed @Refinement("_ != null") short[] array,
      @Refinement("_ >= 0 && _ <= length(array)") int from,
      @Refinement("_ >= from") int to);

  @Modifies("array[*]")
  void fill(
      @Borrowed @Refinement("_ != null") short[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      short value);

  @Modifies({})
  boolean equals(@Borrowed short[] first, @Borrowed short[] second);

  @Modifies({})
  int hashCode(@Borrowed short[] array);

  @Modifies({})
  int compare(@Borrowed short[] first, @Borrowed short[] second);

  @Modifies({})
  @Refinement("_ >= -1 && _ <= length(first) && _ <= length(second)")
  int mismatch(
      @Borrowed @Refinement("_ != null") short[] first,
      @Borrowed @Refinement("_ != null") short[] second);

  @Modifies("array[*]")
  void sort(
      @Borrowed @Refinement("_ != null") short[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to);

  @Modifies({})
  @Refinement("(_ >= from && _ < to) || (_ >= -to - 1 && _ <= -from - 1)")
  int binarySearch(
      @Borrowed @Refinement("_ != null") short[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      short key);

  @Modifies({})
  @Free
  @Refinement("_ != null && length(_) == to - from")
  char[] copyOfRange(
      @Borrowed @Refinement("_ != null") char[] array,
      @Refinement("_ >= 0 && _ <= length(array)") int from,
      @Refinement("_ >= from") int to);

  @Modifies("array[*]")
  void fill(
      @Borrowed @Refinement("_ != null") char[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      char value);

  @Modifies({})
  boolean equals(@Borrowed char[] first, @Borrowed char[] second);

  @Modifies({})
  int hashCode(@Borrowed char[] array);

  @Modifies({})
  int compare(@Borrowed char[] first, @Borrowed char[] second);

  @Modifies({})
  @Refinement("_ >= -1 && _ <= length(first) && _ <= length(second)")
  int mismatch(
      @Borrowed @Refinement("_ != null") char[] first,
      @Borrowed @Refinement("_ != null") char[] second);

  @Modifies("array[*]")
  void sort(
      @Borrowed @Refinement("_ != null") char[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to);

  @Modifies({})
  @Refinement("(_ >= from && _ < to) || (_ >= -to - 1 && _ <= -from - 1)")
  int binarySearch(
      @Borrowed @Refinement("_ != null") char[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      char key);

  @Modifies({})
  @Free
  @Refinement("_ != null && length(_) == to - from")
  int[] copyOfRange(
      @Borrowed @Refinement("_ != null") int[] array,
      @Refinement("_ >= 0 && _ <= length(array)") int from,
      @Refinement("_ >= from") int to);

  @Modifies("array[*]")
  void fill(
      @Borrowed @Refinement("_ != null") int[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      int value);

  @Modifies({})
  boolean equals(@Borrowed int[] first, @Borrowed int[] second);

  @Modifies({})
  int hashCode(@Borrowed int[] array);

  @Modifies({})
  int compare(@Borrowed int[] first, @Borrowed int[] second);

  @Modifies({})
  @Refinement("_ >= -1 && _ <= length(first) && _ <= length(second)")
  int mismatch(
      @Borrowed @Refinement("_ != null") int[] first,
      @Borrowed @Refinement("_ != null") int[] second);

  @Modifies("array[*]")
  void sort(
      @Borrowed @Refinement("_ != null") int[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to);

  @Modifies({})
  @Refinement("(_ >= from && _ < to) || (_ >= -to - 1 && _ <= -from - 1)")
  int binarySearch(
      @Borrowed @Refinement("_ != null") int[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      int key);

  @Modifies({})
  @Free
  @Refinement("_ != null && length(_) == to - from")
  long[] copyOfRange(
      @Borrowed @Refinement("_ != null") long[] array,
      @Refinement("_ >= 0 && _ <= length(array)") int from,
      @Refinement("_ >= from") int to);

  @Modifies("array[*]")
  void fill(
      @Borrowed @Refinement("_ != null") long[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      long value);

  @Modifies({})
  boolean equals(@Borrowed long[] first, @Borrowed long[] second);

  @Modifies({})
  int hashCode(@Borrowed long[] array);

  @Modifies({})
  int compare(@Borrowed long[] first, @Borrowed long[] second);

  @Modifies({})
  @Refinement("_ >= -1 && _ <= length(first) && _ <= length(second)")
  int mismatch(
      @Borrowed @Refinement("_ != null") long[] first,
      @Borrowed @Refinement("_ != null") long[] second);

  @Modifies("array[*]")
  void sort(
      @Borrowed @Refinement("_ != null") long[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to);

  @Modifies({})
  @Refinement("(_ >= from && _ < to) || (_ >= -to - 1 && _ <= -from - 1)")
  int binarySearch(
      @Borrowed @Refinement("_ != null") long[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      long key);

  @Modifies({})
  @Free
  @Refinement("_ != null && length(_) == to - from")
  float[] copyOfRange(
      @Borrowed @Refinement("_ != null") float[] array,
      @Refinement("_ >= 0 && _ <= length(array)") int from,
      @Refinement("_ >= from") int to);

  @Modifies("array[*]")
  void fill(
      @Borrowed @Refinement("_ != null") float[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      float value);

  @Modifies({})
  boolean equals(@Borrowed float[] first, @Borrowed float[] second);

  @Modifies({})
  int hashCode(@Borrowed float[] array);

  @Modifies({})
  int compare(@Borrowed float[] first, @Borrowed float[] second);

  @Modifies({})
  @Refinement("_ >= -1 && _ <= length(first) && _ <= length(second)")
  int mismatch(
      @Borrowed @Refinement("_ != null") float[] first,
      @Borrowed @Refinement("_ != null") float[] second);

  @Modifies("array[*]")
  void sort(
      @Borrowed @Refinement("_ != null") float[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to);

  @Modifies({})
  @Refinement("(_ >= from && _ < to) || (_ >= -to - 1 && _ <= -from - 1)")
  int binarySearch(
      @Borrowed @Refinement("_ != null") float[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      float key);

  @Modifies({})
  @Free
  @Refinement("_ != null && length(_) == to - from")
  double[] copyOfRange(
      @Borrowed @Refinement("_ != null") double[] array,
      @Refinement("_ >= 0 && _ <= length(array)") int from,
      @Refinement("_ >= from") int to);

  @Modifies("array[*]")
  void fill(
      @Borrowed @Refinement("_ != null") double[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      double value);

  @Modifies({})
  boolean equals(@Borrowed double[] first, @Borrowed double[] second);

  @Modifies({})
  int hashCode(@Borrowed double[] array);

  @Modifies({})
  int compare(@Borrowed double[] first, @Borrowed double[] second);

  @Modifies({})
  @Refinement("_ >= -1 && _ <= length(first) && _ <= length(second)")
  int mismatch(
      @Borrowed @Refinement("_ != null") double[] first,
      @Borrowed @Refinement("_ != null") double[] second);

  @Modifies("array[*]")
  void sort(
      @Borrowed @Refinement("_ != null") double[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to);

  @Modifies({})
  @Refinement("(_ >= from && _ < to) || (_ >= -to - 1 && _ <= -from - 1)")
  int binarySearch(
      @Borrowed @Refinement("_ != null") double[] array,
      @Refinement("_ >= 0 && _ <= to && to <= length(array)") int from,
      int to,
      double key);
}
