import specification.Borrowed;

// Investigation reproducer: literal lengths remain unknown; explicit observer effects fix the loop.
// Run with --jdk all. The constructed, guarded and loop methods verify.
class StringFacts {
  static char literal() {
    return "abc".charAt(0); // Expect: REFINEMENT
  }

  static char constructed() {
    String s = new String(new char[] {'a', 'b', 'c'});
    return s.charAt(0);
  }

  static char guarded(@Borrowed String s) {
    if (s.length() > 0) return s.charAt(0);
    return '?';
  }

  static boolean loop(@Borrowed String a, @Borrowed String b) {
    if (a.length() != b.length()) return false;
    for (int i = 0; i < a.length(); i++) {
      char x = a.charAt(i);
      char y = b.charAt(i);
      if (x != y) return false;
    }
    return true;
  }
}
