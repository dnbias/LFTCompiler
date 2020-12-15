public class NumberTok extends Token {
  public int value = 0;
  public NumberTok(String s) {
    super(Tag.NUM);
    value = Integer.parseInt(s);
  }
  public String toString() { return "<" + tag + ", " + value + ">"; }
}
