package exam;
import java.io.Console;
import java.util.Scanner;

/** Line-based input avoids Scanner newline bugs and recovers from invalid choices. */
public final class ConsoleInput {
    private final Scanner scanner=new Scanner(System.in);
    public String read(String prompt) {
        System.out.print(prompt);
        if(!scanner.hasNextLine()) throw new EndOfInput();
        return scanner.nextLine().trim();
    }
    public int choice(String prompt,int min,int max) {
        while(true) {
            try {
                int n=Integer.parseInt(read(prompt));
                if(n>=min && n<=max) return n;
            } catch(NumberFormatException ignored) { }
            System.out.println("Enter a number from "+min+" to "+max+".");
        }
    }
    public long id(String prompt) {
        while(true) {
            try { return Validation.positiveId(Long.parseLong(read(prompt))); }
            catch(IllegalArgumentException e) { System.out.println("Enter a positive numeric ID."); }
        }
    }
    public char[] password(String prompt) {
        Console console=System.console();
        if(console!=null) {
            char[] value=console.readPassword("%s",prompt);
            if(value==null) throw new EndOfInput();
            return value;
        }
        // IDE terminals may not expose Console; disclose that input will be visible.
        System.out.println("Note: password input is visible in this IDE terminal.");
        System.out.print(prompt);
        if(!scanner.hasNextLine()) throw new EndOfInput();
        return scanner.nextLine().toCharArray();
    }
    public static final class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
