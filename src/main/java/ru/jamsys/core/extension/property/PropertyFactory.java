package ru.jamsys.core.extension.property;

import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.extension.property.item.type.PropertyBoolean;
import ru.jamsys.core.extension.property.item.type.PropertyInstance;
import ru.jamsys.core.extension.property.item.type.PropertyInteger;
import ru.jamsys.core.extension.property.item.type.PropertyString;

public class PropertyFactory {

    public static <X> PropertyInstance<X> instanceOf(Class<X> cls, X value) {
        PropertyInstance<?> propertyInstance = null;
        if (ServiceClassFinder.instanceOf(cls, String.class)) {
            propertyInstance = new PropertyString((String) value);
        } else if (ServiceClassFinder.instanceOf(cls, Boolean.class)) {
            propertyInstance = new PropertyBoolean((Boolean) value);
        } else if (ServiceClassFinder.instanceOf(cls, Integer.class)) {
            propertyInstance = new PropertyInteger((Integer) value);
        }
        @SuppressWarnings("unchecked")
        PropertyInstance<X> resultInstance1 = (PropertyInstance<X>) propertyInstance;
        return resultInstance1;
    }

}
