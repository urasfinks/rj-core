package ru.jamsys.component;

import ru.jamsys.App;

public abstract class AbstractComponent implements Component {

    public AbstractComponent() {
        App.context.getBean(Dictionary.class).getMap().put(getClass(), this);
    }

    @Override
    public void run() {

    }

    @Override
    public void shutdown() {

    }

}
