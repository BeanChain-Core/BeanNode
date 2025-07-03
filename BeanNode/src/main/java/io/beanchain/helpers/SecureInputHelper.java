package io.beanchain.helpers;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class SecureInputHelper {

    public static String promptHidden(String promptMessage) {
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .jansi(true)
                    .build();

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            return reader.readLine(promptMessage + ": ", '*');
        } catch (UserInterruptException e) {
            System.out.println("Input cancelled.");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}