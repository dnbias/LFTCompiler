public class myUtils{
    static boolean isValidChar(char c){
        return (Character.isDigit(c) ||
                Character.isLetter(c) ||
                c == '_'); 
    } 
}
