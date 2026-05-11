/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.endpoints;

import java.io.IOException;
import opensrs.SessionHolder;
import opensrs.database.Database;
import opensrs.dataobjects.User;
import org.autumn.State;
import org.autumn.State.Cookie;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromBody;
import org.autumn.annotations.FromCookie;

/**
 *
 * @author piotr
 */
public class Login {

    private Database database;

    public Login(Database database) {
        this.database = database;
    }

    @Endpoint(value = "/login", allowedMethods = {"POST"})
    public void login(
            State state,
            @FromBody("name") String username,
            @FromBody("password") String password,
            @FromBody(value = "redirect", optional = true) String redirect
    ) throws IOException {
        String newSeskey = SessionHolder.login(database, username, password);
        state.setCookie(new Cookie("session", newSeskey).setHttpOnly(true).setPath("/"));
        state.setHeader("Location", redirect == null ? "/" : redirect);
        state.sendResponseHeaders(302);
    }

    @Endpoint("/logout")
    public void logout(
            State state,
            @FromCookie("session") String session
    ) throws IOException {
        state.setCookie(new Cookie("session", "").setHttpOnly(true).setPath("/"));
        state.setHeader("Location", "/");
        state.sendResponseHeaders(302);
    }

    @Endpoint(value = "/register", allowedMethods = {"POST"})
    public void register(
            State state,
            @FromBody("name") String username,
            @FromBody("password") String password,
            @FromBody("confirmPassword") String confirmPassword,
            @FromBody(value = "redirect", optional = true) String redirect) throws IOException {
        if(database.getUserByName(username) != null) {
            state.sendString("User already exists", "text/plain", 400);
        } else {
            if(!password.equals(confirmPassword)) {
                state.sendString("Passwords are not matching!", "text/plain", 400);
            } else {
                database.createUser(User.fromPassword(username, password));
                login(state, username, password, redirect);
            }
        }
    }
}
