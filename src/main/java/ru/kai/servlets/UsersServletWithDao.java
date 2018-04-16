package ru.kai.servlets;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.kai.dao.UsersDao;
import ru.kai.dao.UsersDaoJDBCImpl;
import ru.kai.dao.UsersDaoJDBCTemplateImpl;
import ru.kai.models.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@WebServlet("/users")
public class UsersServletWithDao extends HttpServlet {

    private UsersDao usersDao;
    private PasswordEncoder passwordEncoder;

    @Override
    public void init() throws ServletException {

        DriverManagerDataSource dataSource =
                new DriverManagerDataSource();
        Properties properties = new Properties();

        try {
            //путь к файлу который задеплоился в томкат, а не к тому что в проекте
            properties.load(new FileInputStream(getServletContext().getRealPath("/WEB-INF/classes/db.properties")));
            String dbUrl = properties.getProperty("db.url");
            String dbUsername = properties.getProperty("db.username");
            String dbPassword = properties.getProperty("db.password");
            String driverClassName = properties.getProperty("db.driverClassName");

            dataSource.setUrl(dbUrl);
            dataSource.setUsername(dbUsername);
            dataSource.setPassword(dbPassword);
            dataSource.setDriverClassName(driverClassName);

            //usersDao= new UsersDaoJDBCImpl(dataSource);
            usersDao= new UsersDaoJDBCTemplateImpl(dataSource);
            passwordEncoder= new BCryptPasswordEncoder();


        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

//        Optional<User> user = usersDao.find(1);

        List<User> users = null;
        if (req.getParameter("name") != null) {
            String name = req.getParameter("name");
            users = usersDao.findAllByFirstName(name);
        } else {
            users = usersDao.findAll();
        }

        req.setAttribute("usersFromServer", users);
        req.getServletContext().getRequestDispatcher("/jsp/users.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        String password = req.getParameter("password");
        LocalDate birthDate = LocalDate.parse(req.getParameter("birthDate"));

        System.out.println(name);

        String hashedPassword=passwordEncoder.encode(password);

        User user = new User(name, hashedPassword, birthDate);
        System.out.println(user.getPassword());
        usersDao.save(user);
        //использую redirect чтобы в теле request не сохранилось имя name
        //иначе в повторном doGet вызовется findAllByFirstName
        resp.sendRedirect("/users");
        //doGet(req, resp);
    }
}
