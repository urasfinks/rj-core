package ru.jamsys.core.extension.expiration;

import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.pool.Valid;
import ru.jamsys.core.resource.ResourceCheckException;

public abstract class AbstractExpirationResource
        extends AbstractManagerElement
        implements
        Valid,
        ResourceCheckException {

}
