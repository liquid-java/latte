package api;

import com.google.gson.Gson;
import latte.api.Verification;
import typechecking.LatteException;

/** Compatibility entry point. Analysis never writes or deletes user source directories. */
public final class App {
  private App() {}

  public static void main(String[] arguments) {
    if (arguments.length == 1 && arguments[0].equals("-multi")) {
      try (var input = new java.util.Scanner(System.in)) {
        while (input.hasNextLine()) launcher(input.nextLine(), true);
      }
    } else latte.api.Main.main(arguments);
  }

  public static void launcher(String path, boolean justJson) {
    var result = Verification.verify(java.util.Objects.requireNonNull(path, "Source path"));
    if (result.accepted()) {
      System.out.println(":::SUCCESS::: " + path);
      return;
    }
    if (justJson) {
      System.err.println(new Gson().toJson(result.diagnostics()));
      return;
    }
    var first = result.diagnostics().get(0);
    throw new LatteException(
        first.message(),
        first.file() + ":" + first.line() + ": " + first.message(),
        result.model().getRootPackage());
  }
}
