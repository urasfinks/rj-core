package ru.jamsys.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class ExceptionHandler extends AbstractComponent {

    public ExceptionHandler(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    public void handler(Exception e) {
        e.printStackTrace();
    }

}