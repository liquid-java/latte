package latte.refinement;

import java.util.*;
import java.util.regex.Pattern;

/** A small Pratt parser. Every token must be consumed; malformed annotations cannot become true. */
public final class PredicateParser {
  private static final Pattern TOKEN =
      Pattern.compile(
          "\\s*(-->|<=>|>>>|>>|<<|&&|\\|\\||==|!=|<=|>=|[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*|(?:0[xX][0-9a-fA-F]+|[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)[fFdDlL]?|\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])'|[.()?:,+*/%!~<>&|^=-])");
  private final List<String> tokens;
  private int cursor;

  private PredicateParser(String source) {
    var found = new ArrayList<String>();
    var matcher = TOKEN.matcher(source);
    int offset = 0;
    while (offset < source.length()) {
      if (source.substring(offset).isBlank()) break;
      matcher.region(offset, source.length());
      if (!matcher.lookingAt())
        throw new IllegalArgumentException("Unexpected character at " + offset + " in " + source);
      found.add(matcher.group(1));
      offset = matcher.end();
    }
    tokens = List.copyOf(found);
  }

  public static Term parse(String source) {
    var parser = new PredicateParser(source);
    var term = parser.expression(0);
    if (parser.cursor != parser.tokens.size())
      throw new IllegalArgumentException("Unexpected token " + parser.peek());
    return term;
  }

  private Term expression(int minimum) {
    Term left;
    String token = take();
    if (Set.of("!", "-", "+", "~").contains(token)) left = new Term.Unary(token, expression(13));
    else if (token.equals("(")) {
      left = expression(0);
      expect(")");
    } else if (token.equals("true")) left = Term.TRUE;
    else if (token.equals("false")) left = Term.FALSE;
    else if (token.equals("null")) left = Term.NULL;
    else if (Character.isDigit(token.charAt(0))) left = Term.number(token);
    else if (token.startsWith("\""))
      left =
          new Term.Literal(
              token.substring(1, token.length() - 1).translateEscapes(), Term.Sort.REF);
    else if (token.startsWith("'"))
      left = Term.integer(token.substring(1, token.length() - 1).translateEscapes().charAt(0));
    else if (token.matches("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*")) {
      if (peek().equals("(")) {
        take();
        var arguments = new ArrayList<Term>();
        if (!peek().equals(")")) {
          do {
            arguments.add(expression(0));
          } while (accept(","));
        }
        expect(")");
        if (token.contains(".")) {
          int dot = token.lastIndexOf('.');
          arguments.add(0, new Term.Name(token.substring(0, dot)));
          token = token.substring(dot + 1);
        }
        left = new Term.Call(token, arguments);
      } else left = new Term.Name(token);
    } else throw new IllegalArgumentException("Expected expression, found " + token);
    while (accept(".")) {
      String member = take();
      expect("(");
      var arguments = new ArrayList<Term>();
      arguments.add(left);
      if (!peek().equals(")")) {
        do {
          arguments.add(expression(0));
        } while (accept(","));
      }
      expect(")");
      left = new Term.Call(member, arguments);
    }
    while (cursor < tokens.size()) {
      String operator = peek();
      int precedence = precedence(operator);
      if (precedence < minimum) break;
      take();
      if (operator.equals("?")) {
        var yes = expression(0);
        expect(":");
        var no = expression(precedence);
        left = new Term.Choice(left, yes, no);
      } else {
        var right = expression(precedence + (operator.equals("-->") ? 0 : 1));
        left = new Term.Binary(operator, left, right);
      }
    }
    return left;
  }

  private static int precedence(String operator) {
    return switch (operator) {
      case "?" -> 0;
      case "-->", "<=>" -> 1;
      case "||" -> 2;
      case "&&" -> 3;
      case "|" -> 4;
      case "^" -> 5;
      case "&" -> 6;
      case "==", "!=" -> 7;
      case "<", "<=", ">", ">=" -> 8;
      case "<<", ">>", ">>>" -> 9;
      case "+", "-" -> 10;
      case "*", "/", "%" -> 11;
      default -> -1;
    };
  }

  private String peek() {
    return cursor < tokens.size() ? tokens.get(cursor) : "<end>";
  }

  private String take() {
    if (cursor == tokens.size()) throw new IllegalArgumentException("Incomplete refinement");
    return tokens.get(cursor++);
  }

  private boolean accept(String token) {
    if (!peek().equals(token)) return false;
    cursor++;
    return true;
  }

  private void expect(String token) {
    if (!accept(token))
      throw new IllegalArgumentException("Expected " + token + ", found " + peek());
  }
}
