package ru.jamsys;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public abstract class AbstractCoreComponent implements CoreComponent {

    public static List<CoreComponent> list = new ArrayList<>(); //Так мы будем понимать, какие вообще компоненты у нас загружены

    @SuppressWarnings("unused")
    public AbstractCoreComponent() {
        list.add(this);
    }

    @Override
    public void shutdown() {
        list.remove(this);
    }
}
