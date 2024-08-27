package ru.jamsys.core.flat.template.jdbc;


import ru.jamsys.core.extension.CamelNormalization;

public interface JdbcRequestRepository extends CamelNormalization {

    JdbcTemplate getJdbcTemplate();

}
