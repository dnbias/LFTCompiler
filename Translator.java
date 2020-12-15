import java.io.*;

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

  public void stat(/* completare */) {
    switch (look.tag) {
    // ... completare ...
    case '=':
      match('=');
      match(Tag.ID);
      expr();
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
      if (look.tag == Tag.ID) {
        int id_addr = st.lookupAddress(((Word)look).lexeme);
        if (id_addr == -1) {
          id_addr = count;
          st.insert(((Word)look).lexeme, count++);
        }
        match(Tag.ID);
        match(')');
        code.emit(OpCode.invokestatic, 0);
        code.emit(OpCode.istore, id_addr);
      } else
        error("Error in grammar (stat) after read( with " + look);
      break;
      // ... completare ...

    case Tag.COND:
      match(Tag.COND);
      whenlist();
      match(Tag.ELSE);
      stat();
      break;

    case Tag.WHILE:
      match(Tag.WHILE);
      match('(');
      bexpr();
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
}

private void expr(/* completare */) {
  switch (look.tag) {
  // ... completare ...
  case '-':
    match('-');
    expr();
    expr();
    code.emit(OpCode.isub);
    break;
    // ... completare ...
  }
}

// ... completare ...
}
