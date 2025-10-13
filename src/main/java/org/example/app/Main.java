package org.example.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.util.HibernateUtil;
import org.example.domain.User;
import org.example.dao.UserDaoImpl;
import org.example.service.UserService;
import org.hibernate.SessionFactory;

import java.util.Scanner;

import static org.example.util.InputUtil.*;

public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        SessionFactory sf = null;
        String command;

        try {
            sf = HibernateUtil.getSessionFactory();
            UserService userService = createUserService(sf);
            try (Scanner sc = new Scanner(System.in)) {
                printWelcome();
                command = sc.nextLine();

                while (!command.equals("exit")) {
                    switch (command) {
                        case ("create"):
                            create(sc, userService);
                            break;

                        case ("read"):
                            read(sc, userService);
                            break;

                        case ("update"):
                            update(sc, userService);
                            break;

                        case ("delete"):
                            delete(sc, userService);
                            break;

                        default:
                            printUnknownCommand();
                            break;
                    }
                    command = sc.nextLine();
                }
            }
            log.info("Exiting normally.");
        } catch (Throwable t) {
            log.fatal("Fatal error during startup/run. Exiting with code 1.", t);
            System.exit(1);
        } finally {
            closeSessionFactory(sf);
            log.info("Shutdown complete.");
        }
    }
    private static UserService createUserService(SessionFactory sf) {
        UserDaoImpl userDao = new UserDaoImpl(sf);
        return new UserService(userDao);
    }
    private static void printWelcome() {
        System.out.println("Commands: create | read | update | delete | exit");
    }
    private static void create(Scanner sc, UserService userService) {
        System.out.println("Enter user name");
        String name = readStringField(sc);
        System.out.println("Enter user email");
        String email = readStringField(sc);
        System.out.println("Enter user age");
        Integer age = readAge(sc);

        try {
            userService.saveUser(name, email, age);
            System.out.println("User successfully created");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Create failed: {}", e.getMessage());
            System.out.println(e.getMessage());
        }
    }
    private static void read(Scanner sc, UserService userService) {
        System.out.println("Enter user id");
        Long id = readId(sc);
        try {
            User user = userService.readUser(id);
            System.out.println("User by your id: " + user);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Read failed: {}", e.getMessage());
            System.out.println(e.getMessage());
            System.out.println("Try again");
        }
    }
    private static void update(Scanner sc, UserService userService) {
        System.out.println("Enter user id");
        Long id = readId(sc);
        System.out.println("Enter new name (optional)");
        String name = readStringFieldNull(sc);
        System.out.println("Enter new age (optional)");
        Integer age = readAgeNull(sc);
        System.out.println("Enter new email (optional)");
        String email = readStringFieldNull(sc);
        try {
            User user = userService.updateUser(id, name, email, age);
            System.out.println("Updated user: " + user);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Update failed: {}", ex.getMessage());
            System.out.println(ex.getMessage());
        }
    }
    private static void delete(Scanner sc, UserService userService) {
        System.out.println("Enter user id");
        Long id = readId(sc);
        try {
            userService.removeUserById(id);
            System.out.println("User deleted.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Delete failed: {}", ex.getMessage());
            System.out.println(ex.getMessage());
        }
    }
    private static void printUnknownCommand() {
        System.out.println("Unknown command. Use: create | read | update | delete | exit");
    }
    private static void closeSessionFactory(SessionFactory sf) {
        if (sf != null) {
            try {
                sf.close();
            } catch (Exception e) {
                log.warn("Error closing SessionFactory", e);
            }
        }
    }
}
