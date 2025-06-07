package ru.jamsys.core.component.cron;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class StatisticUploadRepositoryProperty extends RepositoryPropertyAnnotationField<Integer> {

    @SuppressWarnings("all")
    @PropertyKey("batch.max.size.byte")
    @PropertyDescription("Максимальное кол-во байт в пачке на отправку")
    private volatile Integer batchMaxSizeByte = 60;

}
