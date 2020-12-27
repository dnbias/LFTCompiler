import java.io.*;
import org.graalvm.compiler.word.Word.Opcode;

public class Translator {
  private Lexer lex;
  private BufferedReader pbr;
  private Token look;

  SymbolTable st = new SymbolTable();
  CodeGenerator code = new CodeGenerator();
  int count = 0;

  public Translator(Lexer l, BufferedReader br) {
    lex = l;
    pbr = br;
    move();
  }

  void move() {
    look = lex.lexicalScan(pbr);
    System.out.println("token = " + look);
  }

  void error(String s) { throw new Error("near line " + lex.line + ": " + s); }

  void match(int t) {
    if (look.tag == t) {
      if (look.tag != Tag.EOF)
        move();
    } else
      error("Syntax error");
  }

  public void prog() {
    // ... completare ...
    int lnext_prog = code.newLabel();
    statlist(lnext_prog);
    code.emitLabel(lnext_prog);
    match(Tag.EOF);
    try {
      code.toJasmin();
    } catch (java.io.IOException e) {
      System.out.println("IO error\n");
    };
    // ... completare ...
  }

  private void statlist() {
    stat();
    statlist();
  }

  private void statlistp() {
    switch (look.tag) {
    case ';':
      match(';');
      stat();
      statlistp();
      break;
    default:
      // Follow
    }
  }

  public void stat(/* completare */) {
    switch (look.tag) {
    case '=':
      match('=');
      if (look.tag != Tag.ID) {
        error("stat, expected ID found " + look);
      }
      int id_addr = st.lookupAddress(((Word)look).lexeme);
      if (id_addr == -1) {
        id_addr = count;
        st.insert(((Word)look).lexeme, count++);
      }
      match(Tag.ID);
      expr();
      code.emit(OpCode.istore, id_addr);
      break;

    case Tag.PRINT:
      match(Tag.PRINT);
      match('(');
      exprlist();
      match(')');
      break;
    case Tag.READ:
      match(Tag.READ);
      match('(');
      if (look.tag != Tag.ID) {
        error("Error in grammar (stat) after read( with " + look);
      }
      int id_addr = st.lookupAddress(((Word)look).lexeme);
      if (id_addr == -1) {
        id_addr = count;
        st.insert(((Word)look).lexeme, count++);
      }
      match(Tag.ID);
      match(')');
      code.emit(OpCode.invokestatic, 0);
      code.emit(OpCode.istore, id_addr);
      break;

    case Tag.COND:
      match(Tag.COND);
      int cond_f = code.newLabel();
      int cond_exit = code.newLabel();
      whenlist(cond_f);
      match(Tag.ELSE);
      code.emitLabel(cond_f);
      stat();
      code.emitLabel(cond_exit);
      break;

    case Tag.WHILE:
      match(Tag.WHILE);
      int while_t = code.newLabel();
      int while_f = code.newLabel();
      match('(');
      code.emitLabel(while_t);
      bexpr(while_t, while_f);
      code.emitLabel(while_f);
      match(')');
      stat();
      break;

    case '{':
      match('{');
      statlist();
      match('}');
      break;

    default:
      error("Syntax error in stat()");
    }
  }

  private void whenlist(int cond_exit) {
    int whenitem_f = code.newLabel();
    whenitem(whenitem_f);
    code.emit(OpCode.GOto, cond_exit);
    code.emitLabel(whenitem_f);
    whenlistp(cond_exit);
  }

  private void whenlistp(int cond_exit) {
    switch (look.tag) {
    case Tag.WHEN:
      int whenitem_f = code.newLabel();
      whenitem(whenitem_f);
      code.emit(OpCode.GOto, cond_exit);
      code.emitLabel(whenitem_f);
      whenlistp(cond_exit);
      break;
    case Tag.ELSE:
      break;
    default:
      error("whenlistp");
    }
  }

  private void whenitem(int when_f) {
    int when_t = code.newLabel();
    match(Tag.WHEN);
    match('(');
    bexpr(when_t, when_f);
    match(')');
    match(Tag.DO);
    code.emitLabel(when_t);
    stat();
  }

  private void bexpr(int when_t, int when_f) {
    switch (((Word)look).lexeme) {
    case ">":
      match(Tag.RELOP);
      expr();
      expr();
      code.emit(OpCode.if_icmpgt, when_t);
      code.emit(OpCode.GOto, when_f);
      break;
    case ">=":
      match(Tag.RELOP);
      expr();
      expr();
      code.emit(OpCode.if_icmpge, when_t);
      code.emit(OpCode.GOto, when_f);
      break;
    case "<":
      match(Tag.RELOP);
      expr();
      expr();
      code.emit(OpCode.if_icmplt, when_t);
      code.emit(OpCode.GOto, when_f);
      break;
    case "<=":
      match(Tag.RELOP);
      expr();
      expr();
      code.emit(OpCode.if_icmple, when_t);
      code.emit(OpCode.GOto, when_f);
      break;
    case "==":
      match(Tag.RELOP);
      expr();
      expr();
      code.emit(OpCode.if_icmpeq, when_t);
      code.emit(OpCode.GOto, when_f);
      break;
    case "<>":
      match(Tag.RELOP);
      expr();
      expr();
      code.emit(OpCode.if_icmpne, when_t);
      code.emit(OpCode.GOto, when_f);
      break;
    default:
      error("bexpr()");
    }
  }

  private void expr() {
    switch (look.tag) {
    case '+':
      match('+');
      match('(');
      exprlist();
      match(')');
      code.emit(OpCode.iadd);
      break;
    case '-':
      match('-');
      expr();
      expr();
      code.emit(OpCode.isub);
      break;
    case '*':
      match('*');
      match('(');
      exprlist();
      match(')');
      code.emit(OpCode.iadd);
      break;
    case '/':
      match('/');
      expr();
      expr();
      code.emit(OpCode.idiv);
      break;
    case Tag.ID:
      int ID_value = st.lookupAddress(((Word)look).lexeme);
      code.emit(OpCode.iload, ID_value);
      match(Tag.ID);
      break;
    case Tag.NUM:
      code.emit(OpCode.ldc, ((NumberTok)look).value);
      match(Tag.NUM);
      break;
    default:
      error("expr()");
    }
  }

  private void exprlist() {
    expr();
    exprlistp();
  }

  private void exprlistp() {
    switch (look.tag) {
    case ')':
      break;
    default:
      expr();
      exprlistp();
    }
  }
}
