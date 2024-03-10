package ru.jamsys.component;

import org.springframework.context.ApplicationContext;

public abstract class AbstractComponent implements Component {

    public AbstractComponent(ApplicationContext applicationContext) {
        applicationContext.getBean(Dictionary.class).getComponent().put(getClass(), this);
    }

}
