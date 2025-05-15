package ru.jamsys.core.extension.broker.persist;

import ru.jamsys.core.component.manager.Manager;

public interface RiderConfiguration {

    Manager.Configuration<Rider> getRiderConfiguration();

    void setRiderConfiguration(Manager.Configuration<Rider> riderConfiguration);

}
