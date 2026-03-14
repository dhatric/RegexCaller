import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class test {
    public static void main(String[] args) {
        String pattern = "+917096346*";
        StringBuilder sb = new StringBuilder("^");
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '*': sb.append(".*"); break;
                case '?': sb.append("."); break;
                case '.': case '+': case '^': case '$': case '{': case '}': case '[': case ']': case '(': case ')': case '|': case '\\':
                    sb.append("\\").append(c); break;
                default: sb.append(c); break;
            }
        }
        sb.append("$");
        System.out.println("Regex: " + sb.toString());
        boolean matched = Pattern.compile(sb.toString()).matcher("+917096346999").matches();
        System.out.println("Matched: " + matched);
    }
}
