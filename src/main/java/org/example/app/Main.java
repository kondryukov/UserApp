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
        String line, name, email;
        Long id;
        Integer age;
        SessionFactory sf = null;
        try {
            sf = HibernateUtil.getSessionFactory();
            UserDaoImpl userDao = new UserDaoImpl(sf);
            UserService userService = new UserService(userDao);
            try (Scanner sc = new Scanner(System.in)) {
                System.out.println("Commands: create | read | update | delete | exit");
                while (!(line = sc.nextLine()).equals("exit")) {
                    switch (line) {
                        case ("create"):
                            System.out.println("Enter user name");
                            name = readStringField(sc);
                            System.out.println("Enter user email");
                            email = readStringField(sc);
                            System.out.println("Enter user age");
                            age = readAge(sc);

                            try {
                                userService.saveUser(name, email, age);
                                System.out.println("User successfully created");
                            } catch (IllegalArgumentException | IllegalStateException e) {
                                log.warn("Create failed: {}", e.getMessage());
                                System.out.println(e.getMessage());
                                continue;
                            }
                            break;

                        case ("read"):
                            System.out.println("Enter user id");
                            id = readId(sc);
                            User user;
                            try {
                                user = userService.readUser(id);
                            } catch (IllegalArgumentException | IllegalStateException e) {
                                log.warn("Read failed: {}", e.getMessage());
                                System.out.println(e.getMessage());
                                continue;
                            }
                            System.out.println("User by your id: " + user);
                            break;

                        case ("update"):
                            System.out.println("Enter user id");
                            id = readId(sc);
                            System.out.println("Enter new name (optional)");
                            name = readStringFieldNull(sc);
                            System.out.println("Enter new age (optional)");
                            age = readAgeNull(sc);
                            System.out.println("Enter new email (optional)");
                            email = readStringFieldNull(sc);

                            try {
                                userService.updateUser(id, name, email, age);
                                System.out.println("Updated user: " + userService.readUser(id));
                            } catch (IllegalArgumentException | IllegalStateException ex) {
                                log.warn("Update failed: {}", ex.getMessage());
                                System.out.println(ex.getMessage());
                            }
                            break;

                        case ("delete"):
                            System.out.println("Enter user id");
                            id = readId(sc);

                            try {
                                userService.removeUserById(id);
                                System.out.println("Deleted (if existed).");
                            } catch (IllegalArgumentException | IllegalStateException ex) {
                                log.warn("Delete failed: {}", ex.getMessage());
                                System.out.println(ex.getMessage());
                            }
                            break;

                        default:
                            System.out.println("Unknown command. Use: create | read | update | delete | exit");
                            break;
                    }
                }
            }
            log.info("Exiting normally.");
        } catch (Throwable t) {
            log.fatal("Fatal error during startup/run. Exiting with code 1.", t);
            System.exit(1);
        } finally {
            if (sf != null) {
                try {
                    sf.close();
                } catch (Exception e) {
                    log.warn("Error closing SessionFactory", e);
                }
            }
            log.info("Shutdown complete.");
        }
    }
}