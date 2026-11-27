package latte.jdk21.java.lang;

import liquidjava.specification.*;
import specification.*;

/**
 * Integral contracts include the MIN_VALUE absolute-value exception. Exact arithmetic equations
 * hold only on normal return; no floating transcendental approximations are asserted.
 * Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StrictMath.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/StrictMath.java
 */
@ExternalRefinementsFor("java.lang.StrictMath")
public interface StrictMathSpec {
  @Modifies({})
  @Refinement("_ == (a >= b ? a : b)")
  int max(int a, int b);

  @Modifies({})
  @Refinement("_ == (a <= b ? a : b)")
  int min(int a, int b);

  @Modifies({})
  @Refinement("_ == (a == -2147483648 ? a : (a < 0 ? -a : a))")
  int abs(int a);

  @Modifies({})
  @Refinement("_ == (a < 0 ? -a : a) && _ >= 0")
  int absExact(int a);

  @Modifies({})
  @Refinement("_ == a + b")
  int addExact(int a, int b);

  @Modifies({})
  @Refinement("_ == a - b")
  int subtractExact(int a, int b);

  @Modifies({})
  @Refinement("_ == a * b")
  int multiplyExact(int a, int b);

  @Modifies({})
  @Refinement("_ == a + 1")
  int incrementExact(int a);

  @Modifies({})
  @Refinement("_ == a - 1")
  int decrementExact(int a);

  @Modifies({})
  @Refinement("_ == -a")
  int negateExact(int a);

  @Modifies({})
  @Refinement("b > 0 ? (_ >= 0 && _ < b) : (b < 0 && _ <= 0 && _ > b)")
  int floorMod(int a, int b);

  @Modifies({})
  @Refinement("_ == (a >= b ? a : b)")
  long max(long a, long b);

  @Modifies({})
  @Refinement("_ == (a <= b ? a : b)")
  long min(long a, long b);

  @Modifies({})
  @Refinement("_ == (a == -9223372036854775808 ? a : (a < 0 ? -a : a))")
  long abs(long a);

  @Modifies({})
  @Refinement("_ == (a < 0 ? -a : a) && _ >= 0")
  long absExact(long a);

  @Modifies({})
  @Refinement("_ == a + b")
  long addExact(long a, long b);

  @Modifies({})
  @Refinement("_ == a - b")
  long subtractExact(long a, long b);

  @Modifies({})
  @Refinement("_ == a * b")
  long multiplyExact(long a, long b);

  @Modifies({})
  @Refinement("_ == a + 1")
  long incrementExact(long a);

  @Modifies({})
  @Refinement("_ == a - 1")
  long decrementExact(long a);

  @Modifies({})
  @Refinement("_ == -a")
  long negateExact(long a);

  @Modifies({})
  @Refinement("b > 0 ? (_ >= 0 && _ < b) : (b < 0 && _ <= 0 && _ > b)")
  long floorMod(long a, long b);

  @Modifies({})
  @Refinement("_ == a")
  int toIntExact(long a);

  @Refinement("_ >= 0.0 && _ < 1.0")
  double random();

  @Modifies({})
  @Refinement("y > 0 ? (_ * y <= x && x < (_ + 1) * y) : (y < 0 && _ * y >= x && x > (_ + 1) * y)")
  int floorDivExact(int x, int y);

  @Modifies({})
  @Refinement("y > 0 ? ((_ - 1) * y < x && x <= _ * y) : (y < 0 && (_ - 1) * y > x && x >= _ * y)")
  int ceilDivExact(int x, int y);

  @Modifies({})
  @Refinement("y > 0 ? (_ <= 0 && _ > -y) : (y < 0 && _ >= 0 && _ < -y)")
  int ceilMod(int x, int y);

  @Modifies({})
  @Refinement("_ == (value < min ? min : (value > max ? max : value)) && _ >= min && _ <= max")
  int clamp(long value, int min, @Refinement("_ >= min") int max);

  @Modifies({})
  @Refinement("y > 0 ? (_ * y <= x && x < (_ + 1) * y) : (y < 0 && _ * y >= x && x > (_ + 1) * y)")
  long floorDivExact(long x, long y);

  @Modifies({})
  @Refinement("y > 0 ? ((_ - 1) * y < x && x <= _ * y) : (y < 0 && (_ - 1) * y > x && x >= _ * y)")
  long ceilDivExact(long x, long y);

  @Modifies({})
  @Refinement("y > 0 ? (_ <= 0 && _ > -y) : (y < 0 && _ >= 0 && _ < -y)")
  long ceilMod(long x, long y);

  @Modifies({})
  @Refinement("_ == (value < min ? min : (value > max ? max : value)) && _ >= min && _ <= max")
  long clamp(long value, long min, @Refinement("_ >= min") long max);
}
