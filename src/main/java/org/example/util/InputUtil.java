package org.example.util;

import java.util.Scanner;

public final class InputUtil {
    private InputUtil() {}

    public static Long readId(Scanner sc) {
        long id;
        while(true){
            try {
                id = Long.parseLong(sc.nextLine());
                break;
            } catch (NumberFormatException e) {
                System.out.println("Id must be a number");
            }
        }
        return id;
    }

    public static Integer readAge(Scanner sc) {
        int age = -1;
        while(age < 0){
            try {
                age = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException ignored) {

            }
            if (age < 0) {
                System.out.println("Age must be a positive number and not older then 2.1 billion years");
            }
        }
        return age;
    }

    public static Integer readAgeNull(Scanner sc) {
        String line = sc.nextLine().trim();
        if (line.isEmpty()) return null;
        try {
            int age = Integer.parseInt(line);
            if (age >= 0) return age;
        } catch (NumberFormatException ignored) {
        }
        System.out.println("Age must be a positive number and not older than 2.1 billion years");
        return readAge(sc);
    }

    public static String readStringField(Scanner sc) {
        String line;
        while((line = sc.nextLine()).trim().isEmpty()) {
            System.out.println("Field name shouldn't by empty");
        }
        return line;
    }

    public static String readStringFieldNull(Scanner sc) {
        String line = sc.nextLine().trim();
        if (line.isEmpty()) return null;
        return line;
    }
}