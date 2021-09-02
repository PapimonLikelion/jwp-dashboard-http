package nextstep.jwp.framework.manager;

import nextstep.jwp.framework.http.request.HttpRequest;
import nextstep.jwp.framework.http.request.details.HttpMethod;
import nextstep.jwp.framework.manager.annotation.Controller;
import nextstep.jwp.framework.manager.annotation.GetMapping;
import nextstep.jwp.framework.manager.annotation.PostMapping;
import nextstep.jwp.framework.manager.annotation.RequestParam;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class DynamicWebManager {

    private static final String APPLICATION_PATH = "nextstep.jwp.application";
    private static final Logger log = LoggerFactory.getLogger(DynamicWebManager.class);

    private final Set<Object> controllers = new HashSet<>();
    private final Map<HttpRequest, Map<Object, Method>> dynamicWebHandler = new HashMap<>();

    public DynamicWebManager() {
        initializeControllers();
        loadControllerHandler();
    }

    private void initializeControllers() {
        final Reflections reflections = new Reflections(APPLICATION_PATH);
        final Set<Class<?>> annotatedControllers = reflections.getTypesAnnotatedWith(Controller.class);
        for (Class<?> controller : annotatedControllers) {
            registerController(controller);
        }
        log.info("########## annotated controllers loaded ##########");
    }

    private void registerController(final Class<?> controller) {
        try {
            final Constructor<?> constructor = controller.getConstructor();
            controllers.add(constructor.newInstance());
        } catch (Exception e) {
            throw new IllegalArgumentException("Controller 생성 실패");
        }
    }

    private void loadControllerHandler() {
        for (Object controller : controllers) {
            final Class<?> controllerClass = controller.getClass();
            final Method[] methods = controllerClass.getMethods();
            for (Method method : methods) {
                registerGetMethodHandlerIfRequired(controller, method);
                registerPostMethodHandlerIfRequired(controller, method);
            }
        }
        log.info("########## controller handlers loaded ##########");
    }

    private void registerGetMethodHandlerIfRequired(final Object controller, final Method method) {
        final GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (!Objects.isNull(getMapping)) {
            final String requestUrl = getMapping.value();
            dynamicWebHandler.put(HttpRequest.of(HttpMethod.GET, requestUrl), Collections.singletonMap(controller, method));
        }
    }

    private void registerPostMethodHandlerIfRequired(final Object controller, final Method method) {
        final PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (!Objects.isNull(postMapping)) {
            final String requestUrl = postMapping.value();
            dynamicWebHandler.put(HttpRequest.of(HttpMethod.POST, requestUrl), Collections.singletonMap(controller, method));
        }
    }

    public boolean canHandle(final HttpRequest httpRequest) {
        return dynamicWebHandler.containsKey(httpRequest);
    }

    public String handle(final HttpRequest httpRequest) {
        final Map<Object, Method> handler = dynamicWebHandler.get(httpRequest);
        final Object controller = handler.keySet().iterator().next();
        final Method method = handler.get(controller);
        final Object[] parameters = mapMethodParameter(httpRequest, method);

        try {
            final Object result = method.invoke(controller, parameters);
            return String.valueOf(result);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("해당 컨트롤러 메서드에서 오류가 발생했습니다.");
        }
    }

    private Object[] mapMethodParameter(final HttpRequest httpRequest, final Method method) {
        final List<Object> requestParameters = new ArrayList<>();

        for (Parameter parameter : method.getParameters()) {
            final RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (requestParam != null) {
                final String requestParamValue = httpRequest.searchRequestBody(requestParam.value());
                requestParameters.add(requestParamValue);
            }

            if (parameter.getType() == HttpRequest.class) {
                requestParameters.add(httpRequest);
            }
        }
        return requestParameters.toArray(new Object[0]);
    }
}
