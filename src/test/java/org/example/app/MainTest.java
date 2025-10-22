package org.example.app;

import org.example.dao.UserDao;
import org.example.dao.UserDaoImpl;
import org.example.domain.User;
import org.example.service.UserService;
import org.example.util.HibernateUtil;
import org.example.util.InputUtil;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainTest {

    UserDao userDao;
    UserService service;
    PrintStream oldOut;
    InputStream oldIn;

    @BeforeEach
    void captureStdout() {
        userDao = mock(UserDaoImpl.class);
        service = mock(UserService.class);

        oldOut = System.out;
        oldIn = System.in;
    }

    @AfterEach
    void restoreStdout() {
        System.setIn(oldIn);
        System.setOut(oldOut);
    }

    private String runWithInput(String inputScript) {
        InputStream is = new ByteArrayInputStream(inputScript.getBytes());
        System.setIn(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        Scanner sc = new Scanner(System.in);
        Main.run(service, sc);

        return baos.toString(StandardCharsets.UTF_8);
    }

    @Test
    void createTest() {
        when(service.saveUser("name", "mail@mail.ru", 23))
                .thenReturn(new User("name", "mail@mail.ru", 23));

        String script = String.join("\n",
                "create",
                "name",
                "mail@mail.ru",
                "23",
                "exit"
        ) + "\n";


        String stdout = runWithInput(script);

        verify(service).saveUser("name", "mail@mail.ru", 23);

        String[] lines = stdout.split("\n");
        assertThat(lines[0]).isEqualTo("Commands: create | read | update | delete | exit");
        assertThat(lines[1]).isEqualTo("Enter user name");
        assertThat(lines[2]).isEqualTo("Enter user email");
        assertThat(lines[3]).isEqualTo("Enter user age");
        assertThat(lines[4]).isEqualTo("User User{id=null, name='name', email='mail@mail.ru', age=23, createdAt=null, updatedAt=null} successfully created");
    }

    @Test
    void createIllegalArgumentException() {
        when(service.saveUser(any(), any(), anyInt()))
                .thenThrow(new IllegalArgumentException());

        String script = String.join("\n",
                "create",
                "name",
                "mail@mail.ru",
                "23",
                "exit"
        ) + "\n";

        InputStream is = new ByteArrayInputStream(script.getBytes());
        System.setIn(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);

        Scanner sc = new Scanner(System.in);
        Main.create(sc, service);

        String[] lines = baos.toString().split("\n");
        assertThat(lines[0]).isEqualTo("Enter user name");
        assertThat(lines[1]).isEqualTo("Enter user email");
        assertThat(lines[2]).isEqualTo("Enter user age");
        assertThat(lines[3]).isEqualTo("Age must be a positive number and not older then 2.1 billion years");
        assertThat(lines[4]).isEqualTo("Error message: null");
    }

    @Test
    void readTest() {
        when(service.readUser(12L))
                .thenReturn(new User("name", "mail@mail.ru", 23));

        String script = String.join("\n",
                "read",
                "12",
                "exit"
        ) + "\n";

        String stdout = runWithInput(script);

        verify(service).readUser(12L);

        String[] lines = stdout.split("\n");
        assertThat(lines[0]).isEqualTo("Commands: create | read | update | delete | exit");
        assertThat(lines[1]).isEqualTo("Enter user id");
        assertThat(lines[2]).isEqualTo("User by your id: User{id=null, name='name', email='mail@mail.ru', age=23, createdAt=null, updatedAt=null}");
    }

    @Test
    void readIllegalArgumentException() {
        when(service.readUser(any())).thenThrow(new IllegalArgumentException());

        String script = String.join("\n",
                "read",
                "12",
                "exit"
        ) + "\n";

        ByteArrayOutputStream outputStream = inputHelper(script);
        Scanner sc = new Scanner(System.in);
        Main.read(sc, service);

        assertThat(outputStream.toString()).isEqualTo("Enter user id\n" +
                "Id must be a number\n" +
                "null\n" +
                "Try again\n");
    }

    @Test
    void updateTest() {
        when(service.updateUser(12L, null, "new@mail.ru", null))
                .thenReturn(new User("name", "new@mail.ru", 123));

        String script = String.join("\n",
                "update",
                "12",
                "",
                "",
                "new@mail.ru",
                "exit"
        ) + "\n";

        String stdout = runWithInput(script);

        verify(service).updateUser(12L, null, "new@mail.ru", null);

        String[] lines = stdout.split("\n");
        assertThat(lines[0]).isEqualTo("Commands: create | read | update | delete | exit");
        assertThat(lines[1]).isEqualTo("Enter user id");
        assertThat(lines[2]).isEqualTo("Enter new name (optional)");
        assertThat(lines[3]).isEqualTo("Enter new age (optional)");
        assertThat(lines[4]).isEqualTo("Enter new email (optional)");
        assertThat(lines[5]).isEqualTo("Updated user: User{id=null, name='name', email='new@mail.ru', age=123, createdAt=null, updatedAt=null}");
    }

    @Test
    void updateIllegalArgumentException() {
        when(service.updateUser(12L, null, "new@mail.ru", null))
                .thenThrow(new IllegalArgumentException("Update failed"));

        String script = String.join("\n",
                "update",
                "12",
                "",
                "",
                "new@mail.ru",
                "exit"
        ) + "\n";

        ByteArrayOutputStream outputStream = inputHelper(script);
        Scanner sc = new Scanner(System.in);
        Main.run(service, sc);

        verify(service).updateUser(12L, null, "new@mail.ru", null);

        String[] lines = outputStream.toString().split("\n");
        assertThat(lines[0]).isEqualTo("Commands: create | read | update | delete | exit");
        assertThat(lines[1]).isEqualTo("Enter user id");
        assertThat(lines[2]).isEqualTo("Enter new name (optional)");
        assertThat(lines[3]).isEqualTo("Enter new age (optional)");
        assertThat(lines[4]).isEqualTo("Enter new email (optional)");
        assertThat(lines[5]).isEqualTo("Update failed");
    }

    @Test
    void updateIllegalStateException() {
        when(service.updateUser(12L, null, "new@mail.ru", null))
                .thenThrow(new IllegalStateException("Update failed"));

        String script = String.join("\n",
                "update",
                "12",
                "",
                "",
                "new@mail.ru",
                "exit"
        ) + "\n";

        ByteArrayOutputStream outputStream = inputHelper(script);
        Scanner sc = new Scanner(System.in);
        Main.run(service, sc);

        verify(service).updateUser(12L, null, "new@mail.ru", null);

        String[] lines = outputStream.toString().split("\n");
        assertThat(lines[0]).isEqualTo("Commands: create | read | update | delete | exit");
        assertThat(lines[1]).isEqualTo("Enter user id");
        assertThat(lines[2]).isEqualTo("Enter new name (optional)");
        assertThat(lines[3]).isEqualTo("Enter new age (optional)");
        assertThat(lines[4]).isEqualTo("Enter new email (optional)");
        assertThat(lines[5]).isEqualTo("Update failed");
    }

    @Test
    void deleteTest() {
        String script = String.join("\n",
                "delete",
                "12",
                "exit"
        ) + "\n";

        String stdout = runWithInput(script);

        verify(service).removeUserById(12L);

        String[] lines = stdout.split("\n");
        assertThat(lines[0]).isEqualTo("Commands: create | read | update | delete | exit");
        assertThat(lines[1]).isEqualTo("Enter user id");
        assertThat(lines[2]).isEqualTo("User deleted.");
    }

    @Test
    void deleteIllegalArgumentExceptionTest() {
        String script = String.join("\n",
                "delete",
                "12",
                "exit"
        ) + "\n";

        doThrow(new IllegalArgumentException()).when(service).removeUserById(any());

        ByteArrayOutputStream outputStream = inputHelper(script);
        Scanner sc = new Scanner(System.in);
        Main.run(service, sc);

        String[] lines = outputStream.toString().split("\n");
        assertThat(lines[0]).isEqualTo("Commands: create | read | update | delete | exit");
        assertThat(lines[1]).isEqualTo("Enter user id");
        assertThat(lines[2]).isEqualTo("Error while deleting null");

    }

    @Test
    void printUnknownCommandTest() {
        String script = String.join("\n",
                "blablabla",
                "exit"
        ) + "\n";
        String stdout = runWithInput(script);

        String[] lines = stdout.split("\n");
        assertThat(lines[0]).isEqualTo("Commands: create | read | update | delete | exit");
        assertThat(lines[1]).isEqualTo("Unknown command. Use: create | read | update | delete | exit");
    }

    ByteArrayOutputStream inputHelper(String input) {
        InputStream is = new ByteArrayInputStream(input.getBytes());
        System.setIn(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        return baos;
    }

    @Test
    void mainTest() {
        String input = "exit\n";
        ByteArrayOutputStream output = inputHelper(input);

        SessionFactory sf = mock(SessionFactory.class);
        try (MockedStatic<HibernateUtil> st = mockStatic(HibernateUtil.class);
             MockedConstruction<org.example.dao.UserDaoImpl> userDaoMockedConstruction = mockConstruction(org.example.dao.UserDaoImpl.class);
             MockedConstruction<org.example.service.UserService> userServiceMockedConstruction = mockConstruction(org.example.service.UserService.class)) {

            st.when(HibernateUtil::getSessionFactory).thenReturn(sf);
            doThrow(new RuntimeException()).when(sf).close();
            Main.main(new String[0]);

            assertThat(output.toString()).contains("Commands: create | read | update | delete | exit");
            assertThat(output.toString()).contains("Error closing SessionFactory");
            verify(sf).close();
        }
    }

    @Test
    void mainTestSFNull() {
        String input = "exit\n";
        ByteArrayOutputStream output = inputHelper(input);
        try (MockedStatic<HibernateUtil> st = mockStatic(HibernateUtil.class);
             MockedConstruction<org.example.dao.UserDaoImpl> userDaoMockedConstruction = mockConstruction(org.example.dao.UserDaoImpl.class);
             MockedConstruction<org.example.service.UserService> userServiceMockedConstruction = mockConstruction(org.example.service.UserService.class)) {
            st.when(HibernateUtil::getSessionFactory).thenReturn(null);
            Main.main(new String[0]);
            assertThat(output.toString()).contains("Commands: create | read | update | delete | exit");
        }
    }

    @Test
    void readIdTest() {
        ByteArrayOutputStream outputStream = inputHelper("notLongValue\n12\n");
        Scanner sc = new Scanner(System.in);
        Long result = InputUtil.readId(sc);

        assertThat(outputStream.toString()).isEqualTo("Id must be a number\n");
        assertThat(result).isEqualTo(12L);
    }

    @Test
    void readAgeNull() {
        ByteArrayOutputStream outputStream = inputHelper("notIntegerValue\n12\n");
        Scanner sc = new Scanner(System.in);
        InputUtil.readAgeNull(sc);

        assertThat(outputStream.toString()).isEqualTo("Age must be a positive number and not older than 2.1 billion years\n");
    }

    @Test
    void readAgeNullNullValue() {
        inputHelper("\n");
        Scanner sc = new Scanner(System.in);
        Integer result = InputUtil.readAgeNull(sc);

        assertThat(result).isNull();
    }

    @Test
    void readAgeTest() {
        ByteArrayOutputStream outputStream = inputHelper("notIntegerValue\n-12\n12\n");
        Scanner sc = new Scanner(System.in);
        Integer result = InputUtil.readAge(sc);

        String[] lines = outputStream.toString().split("\n");
        assertThat(lines[0]).isEqualTo("Age must be a positive number and not older then 2.1 billion years");
        assertThat(lines[1]).isEqualTo("Age must be a positive number and not older then 2.1 billion years");
        assertThat(result).isEqualTo(12);
    }

    @Test
    void getSessionFactoryTest() {
        var sf = HibernateUtil.getSessionFactory();
        assertThat(sf).isNotNull();
        assertInstanceOf(SessionFactory.class, sf);
        (sf).close();
    }

    @Test
    void readStringFieldTest() {
        ByteArrayOutputStream outputStream = inputHelper("\nname\n");
        String result = InputUtil.readStringField(new Scanner(System.in));

        assertThat(outputStream.toString()).isEqualTo("Field name shouldn't be empty\n");
        assertThat(result).isEqualTo("name");
    }
}