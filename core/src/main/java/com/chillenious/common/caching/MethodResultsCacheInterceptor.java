package com.chillenious.common.caching;

import com.chillenious.common.util.Objects;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.inject.Inject;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Implements AOP caching. Types must be annotated with {@link CacheReturnValue}, and then methods that should have
 * their results cached must be annotated with {@link CacheReturnValue}. To set this all up,
 * install {@link MethodResultsCachingModule}.
 */
public class MethodResultsCacheInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MethodResultsCacheInterceptor.class);

    private static final Object NULL_VALUE_PLACEHOLDER = new Object(); // cache doesn't like nulls, so put a place holder in there

    @Inject
    MethodResultsCaches locator;

    public MethodResultsCacheInterceptor() {
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        CacheReturnValue cacheable = method.getAnnotation(CacheReturnValue.class);
        Cache<String, Object> cache = locator.get(cacheable);
        String key = getCacheKey(invocation);
        Object returnValue = cache.get(key, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    Object ret = invocation.proceed();
                    return ret != null ? ret : NULL_VALUE_PLACEHOLDER;
                } catch (Throwable e) {
                    throw Throwables.propagate(e);
                }
            }
        });
        return returnValue != NULL_VALUE_PLACEHOLDER ? returnValue : null;
    }

    private String getCacheKey(MethodInvocation invocation) {
        return getMethodName(invocation.getMethod()) + ":" + Objects.hashCode(invocation.getArguments());
    }

    private String getMethodName(Method method) {
        StringBuilder methodName = new StringBuilder()
                .append(method.getDeclaringClass().getSimpleName()).append(".").append(method.getName()).append("(");
        Class[] params = method.getParameterTypes();
        for (int j = 0; j < params.length; j++) {
            methodName.append(getTypeName(params[j]));
            if (j < (params.length - 1)) {
                methodName.append(",");
            }
        }
        methodName.append(")");
        return methodName.toString();
    }

    private String getTypeName(Class type) {
        if (type.isArray()) {
            try {
                Class cl = type;
                int dimensions = 0;
                while (cl.isArray()) {
                    dimensions++;
                    cl = cl.getComponentType();
                }
                StringBuilder sb = new StringBuilder();
                sb.append(cl.getName());
                for (int i = 0; i < dimensions; i++) {
                    sb.append("[]");
                }
                return sb.toString();
            } catch (Exception e) { /*FALLTHRU*/ }
        }
        return type.getName();
    }

}
