package com.g10.prs.core.printer;

import com.g10.prs.core.PrsException;

import java.util.Scanner;

/** Contains every functions to get inputs from user. */
public class In {
    /** Scanner for system input. */
    private static final Scanner scanner = new Scanner(System.in);

    /**
     * Gets a string from the user.
     *
     * @param message The message to show.
     * @return the string.
     */
    public static String nextString(String message) {
        String ret;

        while (true) {
            ret = next(message);

            if (!ret.isEmpty()) {
                return ret;
            } else {
                Out.println("Cette valeur n'est pas une chaîne de caractères !");
            }
        }
    }

    /**
     * Gets an integer from the user.
     *
     * @param message The message to show.
     * @return the integer.
     */
    public static int nextInt(String message) {
        int ret;

        while (true) {
            String result = next(message);

            try {
                ret = Integer.parseInt(result);
                return ret;
            } catch (Exception e) {
                Out.println("Cette valeur n'est pas un nombre entier !");
            }
        }
    }

    /**
     * Gets the next answer (to a menu's question) from the user.
     *
     * @param message The message to show.
     * @param canGoBack Defines if he is in a sub-menu (from which he can go back).
     * @param max The maximum value of the answer.
     * @return the answer.
     */
    public static int nextAnswer(String message, boolean canGoBack, int max) {
        int ret;

        while (true) {
            String result = next(message);

            if (result.length() == 1 && result.charAt(0) == 'q') {
                return -1;
            } else if (canGoBack && result.length() == 1 && result.charAt(0) == 'b') {
                return 0;
            } else {
                try {
                    ret = Integer.parseInt(result);

                    if (ret < 1 || ret > max) {
                        throw new PrsException("Réponse invalide !");
                    }

                    return ret;
                } catch (Exception e) {
                    Out.println("Cette valeur n'est pas une réponse valide !");
                }
            }
        }
    }

    /**
     * Gets the next input from the user.
     *
     * @param message The message to show.
     * @return the next input as a string.
     */
    private static String next(String message) {
        String ret;
        Out.print(message);
        ret = scanner.nextLine().trim();
        Out.printToFile(ret.length() == 0 ? "<empty>" : ret);
        Out.simulateNewLine();
        return ret;
    }
}
