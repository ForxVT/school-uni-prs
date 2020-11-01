package com.g10.prs;

import com.g10.prs.model.PetRescueSaga;

/** This is main class of the cli program. */
public class Program {
    /** Start of the program. */
    public static void main(String[] args) {
        // Exit with the error code returned by the Pet Rescue Saga command.
        System.exit(new PetRescueSaga().run(args));
    }
}
