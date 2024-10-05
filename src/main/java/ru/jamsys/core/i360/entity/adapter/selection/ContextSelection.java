package ru.jamsys.core.i360.entity.adapter.selection;

import ru.jamsys.core.i360.Context;

public interface ContextSelection {

    Context transform(Context context, Context contextSelection);

}
