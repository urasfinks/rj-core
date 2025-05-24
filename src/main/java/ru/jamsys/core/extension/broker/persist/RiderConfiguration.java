package ru.jamsys.core.extension.broker.persist;

import ru.jamsys.core.component.manager.ManagerConfiguration;

public interface RiderConfiguration {

    ManagerConfiguration<Rider> getRiderConfiguration();

    void setRiderConfiguration(ManagerConfiguration<Rider> riderConfiguration);

}
