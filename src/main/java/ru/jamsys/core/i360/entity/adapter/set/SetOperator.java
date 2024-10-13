package ru.jamsys.core.i360.entity.adapter.set;

import ru.jamsys.core.i360.Context;

public interface SetOperator {

    Context transform(Context context, Context contextSelection);

}
