/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.autumn;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromBody;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromExtra;
import org.autumn.annotations.FromPath;
import org.autumn.annotations.Route;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.autumn.annotations.Middleware;
import org.autumn.annotations.FromQuery;

/**
 * The base class of Autumn. Contains the business logic of the server
 * @author piotr
 */
public class Server {

    /**
     * The tag used while logging.
     */
    private static final String TAG = "Autumn";
    
    /**
     * The version of the Autumn server.
     */
    public static final String VERSION = "1.1.0";

    
    /**
     * Maps an {@link EndpointInstance} to its route.
     */
    private final Map<String, List<EndpointInstance>> endpoints = new HashMap<>();
    
    /**
     * TODO: ???
     */
    private final Map<String, List<EndpointInstance>> middlewares = new HashMap<>();
    
    /**
     * Determines the priority order of the endpoints.
     * Higher in the list = bigger priority.
     * In situations where multiple endpoints match our querry
     * the one with the hightest priority is selected.
     */
    private final List<String> endpointsHierarchy;

    /**
     * TODO: ???
     */
    private HttpServer underlying = null;

    /**
     * An instance of a class implementing the {@link Logger} interface.
     * Used for logging important info and errors by the server.
     */
    private Logger logger;

    private boolean invokeEndpoint(EndpointInstance endpoint, State state, HttpExchange exchange, Map<String, String> pathParameters, Map<String, Object> bodyParameters) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Map<String, Object> getParameters = Utils.queryToMap(exchange.getRequestURI().getRawQuery());
        Map<String, Object> cookies = Utils.queryToMap(exchange.getRequestHeaders().getFirst("Cookie"), "; ");

        Method toInvoke = endpoint.method;

        Object parameters[] = new Object[toInvoke.getParameterCount()];

        parameters[0] = state;

        endpoint.pathParamsToArgumentsMap.forEach((i, n) -> parameters[i] = pathParameters.get(n));

        if (!endpoint.allowedMethods.contains(exchange.getRequestMethod()) && endpoint.middlewareScope == null) {
            state.sendString("AUTUMN: Invalid request method", 400);
            return false;
        }

        for (int i : endpoint.cookiesToArgumentsMap.keySet()) {
            ParameterInfo in = endpoint.cookiesToArgumentsMap.get(i);
            Object result = cookies.get(in.name);
            if (result == null && !in.optional) {
                try {
                    state.sendString("AUTUMN: Missing mandatory cookie " + in.name, 400);
                    return false;
                } catch (IOException ex) {
                }
            }
            parameters[i] = result;
        }

        for (int i : endpoint.getParamsToArgumentsMap.keySet()) {
            ParameterInfo in = endpoint.getParamsToArgumentsMap.get(i);
            Object result = getParameters.get(in.name);
            if (result == null && !in.optional) {
                try {
                    state.sendString("AUTUMN: Missing mandatory get parameter " + in.name, 400);
                    return false;
                } catch (IOException ex) {
                }
            }
            parameters[i] = result;
        }

        for (int i : endpoint.bodyParamsToArgumentsMap.keySet()) {
            ParameterInfo in = endpoint.bodyParamsToArgumentsMap.get(i);
            if ((bodyParameters == null || bodyParameters.get(in.name) == null) && !in.optional) {
                try {
                    state.sendString("AUTUMN: Missing mandatory body parameter " + in.name, 400);
                    return false;
                } catch (IOException ex) {
                }
            }
            parameters[i] = bodyParameters.get(in.name);
        }
        
        for (int i : endpoint.extraParamsToArgumentsMap.keySet()) {
            ParameterInfo in = endpoint.extraParamsToArgumentsMap.get(i);
            if ((state.getExtra(in.name) == null) && !in.optional) {
                try {
                    state.sendString("AUTUMN: Missing mandatory extra (internal) parameter " + in.name, 400);
                    return false;
                } catch (IOException ex) {
                }
            }
            parameters[i] = state.getExtra(in.name);
        }


        try {
            Object ret = toInvoke.invoke(endpoint.instance, parameters);
            if(endpoint.middlewareScope == null)
                exchange.close();
            if(ret instanceof Boolean && ret == Boolean.FALSE){
                return (Boolean) ret;
            }
            return true;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            try {
                state.sendString("AUTUMN: The underlying endpoint has encountered a fatal error:\n\n\n" + Utils.traceToString(ex), 500);
            } catch (IOException notImportant) {
            }
            logger.logException(Logger.ERROR, TAG, ex, "The underlying endpoint has encountered a fatal error (" + path + ")");
        }
        return false;
    }

    /**
     * Starts the server.
     * @param port - which port should the server start on.
     * @throws IOException
     */
    public void start(int port) throws IOException {
        logger.log(Logger.INFO, TAG, "Starting Autumn server at port " + port);
        underlying = HttpServer.create(new InetSocketAddress(port), 0);
        underlying.setExecutor(Executors.newCachedThreadPool());
        underlying.createContext("/", e -> {
            State state = new State(
                    e.getRequestHeaders(),
                    e.getResponseBody(),
                    e.getRequestBody(),
                    e.getRequestURI(),
                    e.getRequestMethod()) {
                @Override
                public void sendResponseHeaders(int statusCode) throws IOException {
                    e.getResponseHeaders().putAll(getOutboundHeaders());
                    e.sendResponseHeaders(statusCode, outputLength);
                }
            };

            state.setHeader("Connection", "close");
            state.setHeader("X-Powered-By", "Autumn " + Server.VERSION);

            String path = e.getRequestURI().getPath();
            logger.log(Logger.INFO, TAG, "Incoming connection to path " + path);
            try {
                for (String possibleEndpointFormat : endpointsHierarchy) {
                    Map<String, String> pathParameters;
                    Map<String, Object> bodyParameters = null;
                    try {
                        pathParameters = Utils.parse(possibleEndpointFormat, path);
                    } catch (Exception ex) {
                        continue;
                    }
                                        
                    // If it's matching
                    if (pathParameters != null) {
                        // A middleware is just an endpoint which doesn't end the chain of calls
                        List<EndpointInstance> endpointsToInvoke = new LinkedList<>();
                        List<EndpointInstance> matchedEndpoints = endpoints.get(possibleEndpointFormat)
                                .stream()
                                .filter(z -> z.allowedMethods.contains(state.getMethod()))
                                .collect(Collectors.toList());
                        if(matchedEndpoints.isEmpty()) continue;
                        else if(matchedEndpoints.size() != 1) {
                            state.sendString("AUTUMN: More than one endpoint matches this exact path. The server is misconfigured.", 500);
                            logger.log(Logger.WARNING, TAG, "More than one endpoint matches");
                            return;
                        }
                        EndpointInstance thisEndpoint = matchedEndpoints.get(0);
                        // If the method doesn't match, continue
                        if(!thisEndpoint.allowedMethods.contains(state.getMethod())) continue;

                        for(String route : middlewares.keySet()){
                            if(path.startsWith(route))
                                middlewares.get(route)
                                        .stream()
                                        .filter(mw -> 
                                                mw.middlewareScope == Middleware.Scope.GLOBAL ||
                                                (mw.middlewareScope == Middleware.Scope.CLASS && mw.method.getDeclaringClass().equals(thisEndpoint.method.getDeclaringClass())))
                                        .forEach(endpointsToInvoke::add);
                        }
                        endpointsToInvoke.add(thisEndpoint);
                        
                        
                        String contentType = e.getRequestHeaders().getFirst("Content-Type");
                        if (contentType != null && endpointsToInvoke.stream().anyMatch(q -> q.tryParsingBody)) {
                            String tokens[] = contentType.split("; ");
                            String base = tokens[0];
                            String charset = "utf-8";
                            if (tokens.length > 1 && tokens[1].startsWith("charset=")) {
                                charset = tokens[1].substring(8);
                            }
                            switch (base) {
                                case "application/x-www-form-urlencoded":
                                    bodyParameters = Utils.queryToMap(new String(Utils.readFully(e.getRequestBody(), Integer.valueOf(e.getRequestHeaders().getFirst("Content-Length"))), charset));
                                    state.setHasBodyBeenParsed(true);
                                    break;
                            }
                        }

                        // Invoke all the whole tree
                        for(EndpointInstance instance : endpointsToInvoke){
                            if(!invokeEndpoint(instance, state, e, pathParameters, bodyParameters))
                                break;
                        }
                        return;
                    }
                }
            } catch (Exception ex) {
                logger.logException(Logger.ERROR, TAG, ex);
            }
            try {
                logger.log(Logger.WARNING, TAG, "No matching endpoint found for path " + path);
                state.sendString("AUTUMN: No matching endpoint found", 404);
            } catch (IOException ex) {
            }
        });
        underlying.start();
    }

    /**
     * Stops the server.
     */
    public void stop() {
        if (underlying != null) {
            underlying.stop(5);
            underlying = null;
        }
    }

    /**
     * Constructor of the Server Object.
     * @param packageName           the name of root package of the server implementation (in this case it's opensrs).
     * @param logger                instance of a class implementing the {@link Logger} interface.
     * @param constructParameters   TODO
     */
    public Server(String packageName, Logger logger, Object... constructParameters) {
        Reflections base = new Reflections(packageName,
                new TypeAnnotationsScanner(),
                new SubTypesScanner(),
                new MethodAnnotationsScanner(),
                new FieldAnnotationsScanner());

        this.logger = logger;

        Map<String, List<EndpointInstance>> routedEndpoints = new HashMap<>();

        //Step 1 - find all methods tagged by @Endpoint and @Middleware
        Set<Method> methods = base.getMethodsAnnotatedWith(Endpoint.class);
        methods.addAll(base.getMethodsAnnotatedWith(Middleware.class));

        Map<Class<?>, Object> instancesMap = new HashMap<>();
        methods.forEach(method -> {
            Class<?> declaringClass = method.getDeclaringClass();
            Route routeAnno = declaringClass.getAnnotation(Route.class);
            Object instance = null;
            if (instancesMap.containsKey(declaringClass)) {
                instance = instancesMap.get(declaringClass);
            } else {
                Constructor<?>[] constructors = declaringClass.getConstructors();
                if(constructors.length != 1) {
                    throw new IllegalStateException("Class containing an @Endpoint method doesn't have one, single constructor!");
                }
                Constructor<?> constr = constructors[0];
                Object[] constructorParamInstances = new Object[constr.getParameterCount()];
                Class<?>[] constructorParamTypes = constr.getParameterTypes();
                root: for(int i = 0; i<constructorParamInstances.length; i++) {
                    for(Object param : constructParameters) {
                        if(constructorParamTypes[i].isAssignableFrom(param.getClass())) {
                            constructorParamInstances[i] = param;
                            continue root;
                        }
                    }
                    throw new IllegalArgumentException("Cannot find the dependency of type: " + constructorParamTypes[i].getCanonicalName());
                }
                try {
                    instance = constr
                            .newInstance(constructorParamInstances);
                    instancesMap.put(declaringClass, instance);
                } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException ex) {
                    throw new IllegalArgumentException(ex);
                }
            }
            //Now got the instance and method. We can parse the arguments
            EndpointInstance endpoint = new EndpointInstance(instance, method);

            if (!method.getParameters()[0].getType().equals(State.class)) {
                throw new IllegalStateException("Function: " + method.getName() + " delcared in " + declaringClass.toString() + " has to have the first argument of type State");
            }

            for (int i = 1; i < method.getParameterCount(); i++) {
                Parameter param = method.getParameters()[i];
                if (param.getAnnotations().length != 1) {
                    throw new IllegalStateException("Function: " + method.getName() + " delcared in " + declaringClass.toString() + " had an invalid argument");
                }
                FromPath fromPath = param.getAnnotation(FromPath.class);
                FromQuery getParam = param.getAnnotation(FromQuery.class);
                FromCookie cookieParam = param.getAnnotation(FromCookie.class);
                FromBody bodyParam = param.getAnnotation(FromBody.class);
                FromExtra extraParam = param.getAnnotation(FromExtra.class);
                if (fromPath != null) {
                    endpoint.pathParamsToArgumentsMap.put(i, fromPath.value());
                } else if (getParam != null) {
                    endpoint.getParamsToArgumentsMap.put(i, new ParameterInfo(getParam.value(), getParam.optional()));
                } else if (cookieParam != null) {
                    endpoint.cookiesToArgumentsMap.put(i, new ParameterInfo(cookieParam.value(), cookieParam.optional()));
                } else if (bodyParam != null) {
                    endpoint.bodyParamsToArgumentsMap.put(i, new ParameterInfo(bodyParam.value(), bodyParam.optional()));
                } else if (extraParam != null) {
                    endpoint.extraParamsToArgumentsMap.put(i, new ParameterInfo(extraParam.value(), extraParam.optional()));
                }
            }

            if (method.isAnnotationPresent(Endpoint.class)) {
                Endpoint endpointAnno = method.getAnnotation(Endpoint.class);
                endpoint.tryParsingBody = endpointAnno.tryParsingBody();
                endpoint.allowedMethods.addAll(Arrays.asList(endpointAnno.allowedMethods()));
                endpoint.middlewareScope = null;

                String endpointName = endpointAnno.value();
                String route = routeAnno == null ? "" : routeAnno.value();
                if (!endpointName.startsWith("/")) {
                    endpointName = "/" + endpointName;
                }
                if (route.endsWith("/")) {
                    route = route.substring(0, route.length() - 1);
                }
                String key = route + endpointName;
                if(!endpoints.containsKey(key)) endpoints.put(key, new LinkedList<>());
                endpoints.get(route + endpointName).add(endpoint);

                if (routeAnno != null) {
                    String rt = routeAnno.value();
                    if (!routedEndpoints.containsKey(rt)) {
                        routedEndpoints.put(rt, new LinkedList<>());
                    }
                    routedEndpoints.get(rt).add(endpoint);
                }
            } else {
                // A middleware
                if (routeAnno == null) {
                    throw new IllegalStateException("Middleware: " + method.getName() + " delcared in " + declaringClass.toString() + " has to have a parent class annotated with @Route");
                }
                if (!middlewares.containsKey(routeAnno.value())) {
                    middlewares.put(routeAnno.value(), new LinkedList<>());
                }
                
                endpoint.middlewareScope = method.getAnnotation(Middleware.class).scope();
                middlewares.get(routeAnno.value()).add(endpoint);
            }
        });

        endpointsHierarchy = new ArrayList<>(endpoints.size());
        endpoints.keySet()
                .stream()
                .sorted(Utils.hierarchicalSort(
                        (a, b) -> Utils.count(b, "/") - Utils.count(a, "/"),
                        (a, b) -> Utils.count(a, "{") - Utils.count(b, "{"),
                        (a, b) -> b.length() - a.length()
                ))
                .forEachOrdered(endpointsHierarchy::add);
    }

    private class EndpointInstance {

        private Object instance;
        private Method method;
        private boolean tryParsingBody;
        private Map<Integer, ParameterInfo> getParamsToArgumentsMap = new HashMap<>();
        private Map<Integer, String> pathParamsToArgumentsMap = new HashMap<>();
        private Map<Integer, ParameterInfo> cookiesToArgumentsMap = new HashMap<>();
        private Map<Integer, ParameterInfo> bodyParamsToArgumentsMap = new HashMap<>();
        private Map<Integer, ParameterInfo> extraParamsToArgumentsMap = new HashMap<>();
        private List<String> allowedMethods = new ArrayList<>();
        
        private Middleware.Scope middlewareScope = Middleware.Scope.GLOBAL;

        public EndpointInstance(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }
    }

    private class ParameterInfo {

        private String name;
        private boolean optional;

        public ParameterInfo(String name, boolean optional) {
            this.name = name;
            this.optional = optional;
        }

    }
}
