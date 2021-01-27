import java.io.*;
// import org.graalvm.compiler.word.Word.Opcode;

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
    int lnext_prog = code.newLabel();
    statlist(lnext_prog);
    code.emitLabel(lnext_prog);
    match(Tag.EOF);
    try {
      code.toJasmin();
    } catch (java.io.IOException e) {
      System.out.println("IO error\n");
    };
  }

  private void statlist(int lnext) {
    int lnext_stat = code.newLabel();
    stat(lnext_stat);
    code.emitLabel(lnext_stat);
    statlistp(lnext);
  }

  private void statlistp(int lnext) {
    switch (look.tag) {
    case ';':
      match(';');
      int lnext_stat = code.newLabel();
      stat(lnext_stat);
      code.emitLabel(lnext_stat);
      statlistp(lnext);
      break;
    case '}':
      match('}');
      code.emit(OpCode.GOto, lnext);
      break;
    case Tag.EOF:
      match(Tag.EOF);
      code.emit(OpCode.GOto, lnext);
      break;
    default:
      error("statlistp");
    }
  }

  public void stat(int lnext) {
    int id_addr;
    switch (look.tag) {
    case '=':
      match('=');
      if (look.tag != Tag.ID) {
        error("stat, expected ID found " + look);
      }
      id_addr = st.lookupAddress(((Word)look).lexeme);
      if (id_addr == -1) {
        id_addr = count;
        st.insert(((Word)look).lexeme, count++);
      }
      match(Tag.ID);
      expr();
      code.emit(OpCode.istore, id_addr);
      code.emit(OpCode.GOto, lnext);
      break;

    case Tag.PRINT:
      match(Tag.PRINT);
      match('(');
      int n = exprlist();
      for (; n > 0; n--) {
        code.emit(OpCode.invokestatic, 1);
      }
      match(')');

      code.emit(OpCode.GOto, lnext);
      break;
    case Tag.READ:
      match(Tag.READ);
      match('(');
      if (look.tag != Tag.ID) {
        error("Error in grammar (stat) after read() with " + look);
      }
      id_addr = st.lookupAddress(((Word)look).lexeme);
      if (id_addr == -1) {
        id_addr = count;
        st.insert(((Word)look).lexeme, count++);
      }
      match(Tag.ID);
      match(')');
      code.emit(OpCode.invokestatic, 0);
      code.emit(OpCode.istore, id_addr);

      code.emit(OpCode.GOto, lnext);
      break;

    case Tag.COND:
      match(Tag.COND);
      int cond_else = code.newLabel();
      int cond_exit = code.newLabel();
      whenlist(cond_else, cond_exit);
      match(Tag.ELSE);
      code.emitLabel(cond_else);
      stat(cond_exit);
      code.emitLabel(cond_exit);
      code.emit(OpCode.GOto, lnext);
      break;

    case Tag.WHILE:
      match(Tag.WHILE);
      int while_t = code.newLabel();
      match('(');
      code.emitLabel(while_t);
      bexpr(while_t, lnext);
      match(')');
      stat(lnext);
      break;

    case Tag.EOF:
      break;

    case '{':
      match('{');
      statlist(lnext);
      // match('}');
      break;

    default:
      error("Syntax error in stat()");
    }
  }

  private void whenlist(int cond_else, int cond_exit) {
    int whenitem_f = code.newLabel();
    whenitem(whenitem_f);
    code.emit(OpCode.GOto, cond_exit);
    code.emitLabel(whenitem_f);
    whenlistp(cond_exit);
    code.emit(OpCode.GOto, cond_else);
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
    int lnext = code.newLabel();
    stat(lnext);
    code.emitLabel(lnext);
  }

  private void bexpr(int b_true, int b_false) {
    int b1_true, b1_false;
    switch (look.tag) {
    case Tag.BOOLEAN:
      switch (((Word)look).lexeme) {
      case "true":
        match(Tag.BOOLEAN);
        code.emit(OpCode.GOto, b_true);
        break;
      case "false":
        match(Tag.BOOLEAN);
        code.emit(OpCode.GOto, b_false);
        break;
      }
      break;
    case Tag.RELOP:
      switch (((Word)look).lexeme) {
      case ">":
        match(Tag.RELOP);
        expr();
        expr();
        code.emit(OpCode.if_icmple, b_false);
        // code.emit(OpCode.GOto, b_false);
        break;
      case ">=":
        match(Tag.RELOP);
        expr();
        expr();
        code.emit(OpCode.if_icmplt, b_false);
        // code.emit(OpCode.GOto, b_false);
        break;
      case "<":
        match(Tag.RELOP);
        expr();
        expr();
        code.emit(OpCode.if_icmpge, b_false);
        // code.emit(OpCode.GOto, b_false);
        break;
      case "<=":
        match(Tag.RELOP);
        expr();
        expr();
        code.emit(OpCode.if_icmpgt, b_false);
        // code.emit(OpCode.GOto, b_false);
        break;
      case "==":
        match(Tag.RELOP);
        expr();
        expr();
        code.emit(OpCode.if_icmpne, b_false);
        // code.emit(OpCode.GOto, b_false);
        break;
      case "<>":
        match(Tag.RELOP);
        expr();
        expr();
        code.emit(OpCode.if_icmpeq, b_false);
        // code.emit(OpCode.GOto, b_false);
        break;
      }
      break;
    case '!':
      match('!');
      bexpr(b_false, b_true);
      code.emit(OpCode.GOto, b_false);
      break;
    case Tag.AND:
      match(Tag.AND);
      b1_true = code.newLabel();
      match('(');
      bexpr(b1_true, b_false);
      code.emit(OpCode.GOto, b1_true);
      match(')');
      code.emitLabel(b1_true);
      match('(');
      bexpr(b_true, b_false);
      // code.emit(OpCode.GOto, b_true);
      match(')');
      break;
    case Tag.OR:
      match(Tag.OR);
      b1_false = code.newLabel();
      match('(');
      bexpr(b_true, b1_false);
      code.emit(OpCode.GOto, b_true);
      match(')');
      code.emitLabel(b1_false);
      match('(');
      bexpr(b_true, b_false);
      // code.emit(OpCode.GOto, b_true);
      match(')');
      break;
    default:
      error("bexpr()");
    }
  }

  private void expr() {
    int n;
    switch (look.tag) {
    case '+':
      match('+');
      match('(');
      n = exprlist() - 1;
      match(')');
      for (; n > 0; n--) {
        code.emit(OpCode.iadd);
      }
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
      n = exprlist() - 1;
      match(')');
      for (; n > 0; n--) {
        code.emit(OpCode.imul);
      }
      break;
    case '/':
      match('/');
      expr();
      expr();
      code.emit(OpCode.idiv);
      break;
    case Tag.ID:
      int ID_addr = st.lookupAddress(((Word)look).lexeme);
      code.emit(OpCode.iload, ID_addr);
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

  private int exprlist() {
    int n_expr = 0;
    expr();
    n_expr = 1 + exprlistp();
    return n_expr;
  }

  private int exprlistp() {
    int n_expr = 0;
    switch (look.tag) {
    case ')':
      return n_expr;
    default:
      expr();
      n_expr = 1 + exprlistp();
      return n_expr;
    }
  }

  public static void main(String[] args) {
    String path = args[0];
    if (!(path.matches("\\w*.lft"))) {
      System.out.println("LFT Compiler - UniTo 2020");
      System.out.println("Usage:\n"
                         + "\tjava Translator [filename].lft\n"
                         + "Output:\n"
                         + "\tOutput.j");
      return;
    }
    try {
      BufferedReader br = new BufferedReader(new FileReader(path));
      Lexer lex = new Lexer();
      Translator tr = new Translator(lex, br);
      tr.prog();
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("Source code translated in Output.j");
    return;
  }
}
