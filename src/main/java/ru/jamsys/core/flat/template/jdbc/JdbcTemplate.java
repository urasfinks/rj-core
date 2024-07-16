package ru.jamsys.core.flat.template.jdbc;


import ru.jamsys.core.extension.CamelNormalization;

public interface JdbcTemplate extends CamelNormalization {

    TemplateJdbc getTemplate();

}
