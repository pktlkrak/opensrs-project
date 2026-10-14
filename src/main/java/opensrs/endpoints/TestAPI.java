/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package opensrs.endpoints;

import java.io.IOException;
import org.autumn.State;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromExtra;
import org.autumn.annotations.Route;
import org.autumn.annotations.FromQuery;
import org.autumn.annotations.Middleware;

/**
 *
 * @author asivery
 */
@Route("/api/")
public class TestAPI {
    @Middleware(scope = Middleware.Scope.CLASS)
    public void middleware(State state) {
        state.setExtra("extra", "Hello!!");
    }
    @Endpoint("hello")
    public void sayHelloGet(State state, @FromExtra("extra") String extra) throws IOException {
        state.sendString("Hello! (GET REQUEST): " + extra, "text/plain");
    }
}
