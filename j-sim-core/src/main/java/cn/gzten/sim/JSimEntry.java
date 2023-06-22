package cn.gzten.sim;

import cn.gzten.pojo.RequestMethod;
import cn.gzten.pojo.SimContext;
import lombok.Data;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class JSimEntry {
    protected Map<Class, Map<String, Object>> beans = new ConcurrentHashMap<>();

    abstract public Object getBean(String beanName);

    abstract public void init();

    /**
     * Used for compile time generation, it will be run during application start up.
     * @param clazz
     * @param beanName
     * @param bean
     */
    public void fillBean(Class clazz, String beanName, Object bean) {
        Map<String, Object> m;
        if (beans.containsKey(clazz)) {
            m = beans.get(clazz);
            if (m.containsKey(beanName)) {
                var errMsg = "Bean " + clazz.getCanonicalName() + " already exists!";
                System.err.println(errMsg);
                throw new RuntimeException(errMsg);
            }
        } else {
            m = new ConcurrentHashMap<>();
            beans.put(clazz, m);
        }
        m.put(beanName, bean);
    }

    abstract public void tryProcessRoute(SimContext ctx);
}
