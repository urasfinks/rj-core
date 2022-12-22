package ru.jamsys;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCoreComponent implements CoreComponent {

    public static List<CoreComponent> list = new ArrayList<>(); //Так мы будем понимать, какие вообще компоненты у нас загружены

    public AbstractCoreComponent() {
        list.add(this);
    }

    @Override
    public void shutdown() {
        list.remove(this);
    }
}
