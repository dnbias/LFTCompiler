import java.io.*;
import java.util.*;

public class Lexer {
  public static int line = 1;
  private char peek = ' ';

  private void readch(BufferedReader br) {
    try {
      peek = (char)br.read();
    } catch (IOException exc) {
      peek = (char)-1;
    }
  }

  public Token lexicalScan(BufferedReader br) {
    while (peek == ' ' || peek == '\t' || peek == '\n' || peek == '\r') {
      if (peek == '\n')
        line++;
      readch(br);
    }

    switch (peek) {
    case '!':
      peek = ' ';
      return Token.not;
    case '(':
      peek = ' ';
      return Token.lpt;
    case ')':
      peek = ' ';
      return Token.rpt;
    case '{':
      peek = ' ';
      return Token.lpg;
    case '}':
      peek = ' ';
      return Token.rpg;
    case '+':
      peek = ' ';
      return Token.plus;
    case '-':
      peek = ' ';
      return Token.minus;
    case '*':
      peek = ' ';
      return Token.mult;
    case '/':
      readch(br);
      switch (peek) {
      case '/':
        do {
          readch(br);
        } while (peek != '\n');
        line++;
        return lexicalScan(br);
      case '*':
        int state = 0;
        Automaton a = new Automaton(state, peek);
        while (state >= 0) {
          readch(br);
          if (peek == (char)-1) {
            error("Invalid Comment, reached EOF\n", peek);
            return null;
          } else if (peek == '\n') {
            line++;
          }
          a.reset(state, peek);
          state = a.comment();
        }
        peek = ' ';
        return lexicalScan(br);
      default:
        // peek = ' ';
        return Token.div;
      }
    case '=':
      readch(br);
      if (peek == '=') {
        peek = ' ';
        return Word.eq;
      } else {
        return Token.assign;
      }
    case ';':
      peek = ' ';
      return Token.semicolon;
    case '&':
      readch(br);
      if (peek == '&') {
        peek = ' ';
        return Word.and;
      } else {
        error("Erroneous character after &", peek);
        return null;
      }
    case '|':
      readch(br);
      if (peek == '|') {
        peek = ' ';
        return Word.or;
      } else {
        error("Erroneous character after |", peek);
        return null;
      }
    case '<':
      readch(br);
      if (peek == '=') {
        peek = ' ';
        return Word.le;
      } else if (peek == '>') {
        peek = ' ';
        return Word.ne;
      } else {
        return Word.lt;
      }
    case '>':
      readch(br);
      if (peek == '=') {
        peek = ' ';
        return Word.ge;
      } else {
        return Word.gt;
      }
    case (char)-1:
      return new Token(Tag.EOF);
    default:
      if (Character.isLetter(peek) ||
          peek == '_') { // DONE: identificatori da aggiornare
        String identifier = "";
        int state = 0;
        while (state >= 0) {
          switch (state) {
          case 0:
            if (Character.isLetter(peek)) {
              state = 1;
            } else {
              state = 2;
            }
            break;
          case 1:
            if (!myUtils.isValidChar(peek)) {
              state = -1;
            }
            break;
          case 2:
            if (peek == '_') {
              state = 2;
            } else if (Character.isLetter(peek) || Character.isDigit(peek)) {
              state = 1;
            } else { // TODO: implement Exceptions
              error("Invalid identifier", identifier);
              return null;
            }
          }
          if (state != -1) {
            identifier += peek;
            readch(br);
          }
        }
        switch (identifier) {
        case "cond":
          return Word.cond;
        case "when":
          return Word.when;
        case "then":
          return Word.then;
        case "else":
          return Word.elsetok;
        case "while":
          return Word.whiletok;
        case "do":
          return Word.dotok;
        case "print":
          return Word.print;
        case "read":
          return Word.read;
        default:
          return new Word(Tag.ID, identifier);
        }
      } else if (Character.isDigit(peek)) {
        String number = "";
        int state = 0;
        while (state >= 0) {
          switch (state) {
          case 0:
            if (peek == '0') {
              number += peek;
              state = -1;
              peek = ' ';
            } else {
              state = 1;
            }
            break;
          case 1:
            if (!Character.isDigit(peek)) {
              state = -1;
            }
          }
          if (state != -1) {
            number += peek;
            readch(br);
          }
        }
        return new NumberTok(number);
      } else {
        error("Unrecognised character", peek);
        return null;
      }
    }
  }

  private void error(String s, char c) {
    throw new Error("Lexer Error At line " + line + ": " + s +
                    "\nLast character: " + c);
  }

  private void error(String s, String id) {
    throw new Error("Lexer Error At line " + line + ": " + s +
                    "\nLast Identifier: " + id);
  }

  public static void main(String[] args) {
    Lexer lex = new Lexer();
    String path = args[0];
    try {
      BufferedReader br = new BufferedReader(new FileReader(path));
      Token tok;
      do {
        tok = lex.lexicalScan(br);
        if (tok.tag != ' ') {
          System.out.println("Scan: " + tok);
        }
      } while (tok.tag != Tag.EOF);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
