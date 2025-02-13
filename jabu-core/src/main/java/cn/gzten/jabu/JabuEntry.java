package cn.gzten.jabu;

import cn.gzten.jabu.core.JabuContext;
import cn.gzten.jabu.util.JsonPop;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class JabuEntry {

    protected Map<String, Object> beans = new ConcurrentHashMap<>();

    @Setter
    protected JsonPop properties = null;

    abstract public Object getBean(String beanName);

    public Object getBeanInMap(String name) {
        return beans.get(name);
    }

    abstract public void init();

    /**
     * Used for compile time generation, it will be run during application start up.
     * @param beanName
     * @param bean
     */
    public void fillBean(String beanName, Object bean) {
        if (beans.containsKey(beanName)) {
            var errMsg = "Bean %s already exists!".formatted(beanName);
            System.err.println(errMsg);
            throw new RuntimeException(errMsg);
        }
        beans.put(beanName, bean);
    }

    abstract public boolean tryProcessRoute(JabuContext ctx);
}
