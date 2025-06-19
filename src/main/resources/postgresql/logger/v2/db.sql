CREATE SEQUENCE IF NOT EXISTS logs_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 100;

-- DROP TABLE IF EXISTS public.logs;

CREATE TABLE IF NOT EXISTS public.logs (
    log_id bigint NOT NULL DEFAULT nextval('logs_log_id_seq'),
    log_uuid uuid NOT NULL,
    log_timestamp timestamp WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    message text NOT NULL COLLATE pg_catalog."default",
    CONSTRAINT logs_pkey PRIMARY KEY (log_id, log_timestamp)
) PARTITION BY RANGE (log_timestamp);

-- Комментарии для таблицы logs
COMMENT ON TABLE public.logs IS 'Основная таблица логов с уникальными записями событий.';

COMMENT ON COLUMN public.logs.log_id IS 'Уникальный идентификатор записи лога, генерируется последовательностью.';
COMMENT ON COLUMN public.logs.log_uuid IS 'Уникальный UUID события для глобальной идентификации.';
COMMENT ON COLUMN public.logs.log_timestamp IS 'Временная метка события, используется для партицирования и фильтрации.';
COMMENT ON COLUMN public.logs.message IS 'Текстовое сообщение лога, содержащее детали события.';

CREATE SEQUENCE IF NOT EXISTS tags_tag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 100;

-- DROP TABLE IF EXISTS public.tags;

CREATE TABLE IF NOT EXISTS public.tags (
    tag_id bigint NOT NULL DEFAULT nextval('tags_tag_id_seq'),
    tag_timestamp timestamp WITHOUT TIME ZONE NOT NULL DEFAULT date_trunc('day', now()),
    name varchar(100) NOT NULL,
    value varchar(255) NOT NULL,
    CONSTRAINT tags_pkey PRIMARY KEY (tag_id, tag_timestamp),
    CONSTRAINT tags_name_value_key UNIQUE (name, value, tag_timestamp)
) PARTITION BY RANGE (tag_timestamp);

-- Комментарии для таблицы tags
COMMENT ON TABLE public.tags IS 'Таблица тегов, связанных с логами, с уникальным именем и значением.';

COMMENT ON COLUMN public.tags.tag_id IS 'Уникальный идентификатор тега, генерируется последовательностью.';
COMMENT ON COLUMN public.tags.tag_timestamp IS 'Временная метка тега, используется для партицирования.';
COMMENT ON COLUMN public.tags.name IS 'Имя тега, например, категория или тип.';
COMMENT ON COLUMN public.tags.value IS 'Значение тега, связанное с именем.';

-- DROP TABLE IF EXISTS public.log_tags;

CREATE TABLE IF NOT EXISTS public.log_tags (
    log_id bigint NOT NULL,
    log_timestamp timestamp WITHOUT TIME ZONE NOT NULL,
    tag_id bigint NOT NULL,
    tag_timestamp timestamp WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT log_tags_pkey PRIMARY KEY (log_id, log_timestamp, tag_id, tag_timestamp),

    CONSTRAINT log_tags_log_fkey FOREIGN KEY (log_id, log_timestamp)
        REFERENCES public.logs (log_id, log_timestamp)
        ON DELETE CASCADE,

    CONSTRAINT log_tags_tag_fkey FOREIGN KEY (tag_id, tag_timestamp)
        REFERENCES public.tags (tag_id, tag_timestamp)
        ON DELETE CASCADE
) PARTITION BY RANGE (log_timestamp);

-- Комментарии для таблицы log_tags
COMMENT ON TABLE public.log_tags IS 'Таблица связей между логами и тегами для реализации многие-ко-многим.';

COMMENT ON COLUMN public.log_tags.log_id IS 'Идентификатор лога, ссылается на logs.log_id.';
COMMENT ON COLUMN public.log_tags.log_timestamp IS 'Временная метка лога, ссылается на logs.log_timestamp.';
COMMENT ON COLUMN public.log_tags.tag_id IS 'Идентификатор тега, ссылается на tags.tag_id.';
COMMENT ON COLUMN public.log_tags.tag_timestamp IS 'Временная метка тега, ссылается на tags.tag_timestamp.';

CREATE OR REPLACE PROCEDURE create_partitions_logs(from_date timestamp without time zone, days integer)
LANGUAGE plpgsql
AS $$
DECLARE
    table_name TEXT := 'logs';
    prefix TEXT;
    left_bound TEXT;
    right_bound TEXT;
    i INTEGER := 0;
BEGIN
    IF days <= 0 THEN
        RETURN;
    END IF;

    WHILE i < days LOOP
        prefix := table_name || '_' || to_char(from_date + i * INTERVAL '1 day', 'YYYYMMDD');
        left_bound := to_char(from_date + i * INTERVAL '1 day', 'YYYY-MM-DD');
        right_bound := to_char(from_date + (i + 1) * INTERVAL '1 day', 'YYYY-MM-DD');

        -- Проверка существования партиции через pg_partition_tree
        IF prefix IN (SELECT child.relname AS part
                      FROM pg_inherits
                               JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
                               JOIN pg_class child ON pg_inherits.inhrelid = child.oid
                               JOIN pg_namespace nmsp_parent ON nmsp_parent.oid = parent.relnamespace
                               JOIN pg_namespace nmsp_child ON nmsp_child.oid = child.relnamespace
                      WHERE parent.relname = table_name) THEN
            i := i + 1;
            CONTINUE;
        END IF;

        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I (LIKE %I INCLUDING ALL) TABLESPACE pg_default;',
            prefix, table_name
        );
        EXECUTE format(
            'ALTER TABLE %I ADD CONSTRAINT %I CHECK (log_timestamp >= %L::timestamp AND log_timestamp < %L::timestamp);',
            prefix, prefix || '_check_bounds', left_bound, right_bound
        );

        -- Индекс по log_timestamp — ускоряет фильтрацию логов по времени
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS %I ON %I (log_timestamp);',
            prefix || '_log_timestamp_idx', prefix
        );

        -- Индекс по log_uuid — ускоряет поиск логов по уникальному идентификатору события
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS %I ON %I (log_uuid);',
            prefix || '_log_uuid_idx', prefix
        );

        EXECUTE format(
            'ALTER TABLE %I ATTACH PARTITION %I FOR VALUES FROM (%L) TO (%L);',
            table_name, prefix, left_bound, right_bound
        );

        i := i + 1;
    END LOOP;
END;
$$;


CREATE OR REPLACE PROCEDURE create_partitions_tags(from_date timestamp without time zone, days integer)
LANGUAGE plpgsql
AS $$
DECLARE
    table_name TEXT := 'tags';
    prefix TEXT;
    left_bound TEXT;
    right_bound TEXT;
    i INTEGER := 0;
BEGIN
    IF days <= 0 THEN
        RETURN;
    END IF;

    WHILE i < days LOOP
        prefix := table_name || '_' || to_char(from_date + i * INTERVAL '1 day', 'YYYYMMDD');
        left_bound := to_char(from_date + i * INTERVAL '1 day', 'YYYY-MM-DD');
        right_bound := to_char(from_date + (i + 1) * INTERVAL '1 day', 'YYYY-MM-DD');

        -- Проверка существования партиции через pg_partition_tree
        IF prefix IN (SELECT child.relname AS part
                      FROM pg_inherits
                               JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
                               JOIN pg_class child ON pg_inherits.inhrelid = child.oid
                               JOIN pg_namespace nmsp_parent ON nmsp_parent.oid = parent.relnamespace
                               JOIN pg_namespace nmsp_child ON nmsp_child.oid = child.relnamespace
                      WHERE parent.relname = table_name) THEN
            i := i + 1;
            CONTINUE;
        END IF;

        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I (LIKE %I INCLUDING ALL) TABLESPACE pg_default;',
            prefix, table_name
        );
        EXECUTE format(
            'ALTER TABLE %I ADD CONSTRAINT %I CHECK (tag_timestamp >= %L::timestamp AND tag_timestamp < %L::timestamp);',
            prefix, prefix || '_check_bounds', left_bound, right_bound
        );

        -- Индекс по tag_timestamp — ускоряет фильтрацию тегов по времени
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS %I ON %I (tag_timestamp);',
            prefix || '_tag_timestamp_idx', prefix
        );

        -- Индекс по (name, value, tag_timestamp) — обеспечивает уникальность и ускоряет поиск
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS %I ON %I (name, value, tag_timestamp);',
            prefix || '_name_value_timestamp_idx', prefix
        );

        -- Доп. индекс по (name, value) — ускоряет поиск без учёта времени
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS %I ON %I (name, value);',
            prefix || '_name_value_idx', prefix
        );

        EXECUTE format(
            'ALTER TABLE %I ATTACH PARTITION %I FOR VALUES FROM (%L) TO (%L);',
            table_name, prefix, left_bound, right_bound
        );

        i := i + 1;
    END LOOP;
END;
$$;


 CREATE OR REPLACE PROCEDURE create_partitions_log_tags(from_date timestamp without time zone, days integer)
 LANGUAGE plpgsql
 AS $$
 DECLARE
     table_name TEXT := 'log_tags';
     prefix TEXT;
     left_bound TEXT;
     right_bound TEXT;
     i INTEGER := 0;
 BEGIN
     IF days <= 0 THEN
         RETURN;
     END IF;

     WHILE i < days LOOP
         prefix := table_name || '_' || to_char(from_date + i * INTERVAL '1 day', 'YYYYMMDD');
         left_bound := to_char(from_date + i * INTERVAL '1 day', 'YYYY-MM-DD');
         right_bound := to_char(from_date + (i + 1) * INTERVAL '1 day', 'YYYY-MM-DD');

         -- Проверка существования партиции через pg_partition_tree
         IF prefix IN (SELECT child.relname AS part
                       FROM pg_inherits
                                JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
                                JOIN pg_class child ON pg_inherits.inhrelid = child.oid
                                JOIN pg_namespace nmsp_parent ON nmsp_parent.oid = parent.relnamespace
                                JOIN pg_namespace nmsp_child ON nmsp_child.oid = child.relnamespace
                       WHERE parent.relname = table_name) THEN
             i := i + 1;
             CONTINUE;
         END IF;

         EXECUTE format(
             'CREATE TABLE IF NOT EXISTS %I (LIKE %I INCLUDING ALL) TABLESPACE pg_default;',
             prefix, table_name
         );
         EXECUTE format(
             'ALTER TABLE %I ADD CONSTRAINT %I CHECK (log_timestamp >= %L::timestamp AND log_timestamp < %L::timestamp);',
             prefix, prefix || '_check_bounds', left_bound, right_bound
         );

         -- Индекс по (log_id, log_timestamp) — ускоряет соединение с logs
         EXECUTE format(
             'CREATE INDEX IF NOT EXISTS %I ON %I (tag_id, tag_timestamp);',
             prefix || '_idx_tag_id_timestamp', prefix
         );

         EXECUTE format(
             'CREATE INDEX IF NOT EXISTS %I ON %I (log_id, log_timestamp);',
             prefix || '_idx_log_id_timestamp', prefix
         );

         EXECUTE format(
             'ALTER TABLE %I ATTACH PARTITION %I FOR VALUES FROM (%L) TO (%L);',
             table_name, prefix, left_bound, right_bound
         );

         i := i + 1;
     END LOOP;
 END;
 $$;

CREATE OR REPLACE PROCEDURE create_partitions(
    from_date timestamp without time zone,
    days integer
)
LANGUAGE plpgsql
AS $$
BEGIN
    CALL create_partitions_logs(from_date, days);
    CALL create_partitions_tags(from_date, days);
    CALL create_partitions_log_tags(from_date, days);
END;
$$;
