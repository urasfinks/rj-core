CREATE TABLE IF NOT EXISTS public.logs (
    log_uuid uuid NOT NULL,
    log_timestamp timestamp WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    message text COLLATE pg_catalog."default",
    CONSTRAINT logs_pkey PRIMARY KEY (log_uuid, log_timestamp)
) PARTITION BY RANGE (log_timestamp);

CREATE TABLE public.tags (
    tag_uuid uuid NOT NULL,
    tag_timestamp timestamp WITHOUT TIME ZONE NOT NULL DEFAULT date_trunc('day', now()),
    name varchar(100) NOT NULL,
    value varchar(255) NOT NULL,
    CONSTRAINT tags_pkey PRIMARY KEY (tag_uuid, tag_timestamp),
    CONSTRAINT tags_name_value_key UNIQUE (name, value, tag_timestamp)
) PARTITION BY RANGE (tag_timestamp);

--Это ускорит SELECT tag_uuid FROM tags WHERE name = 'env' AND value = 'prod' AND tag_timestamp = '2025-06-19'
CREATE INDEX IF NOT EXISTS idx_tags_name_value_timestamp
    ON public.tags (name, value, tag_timestamp);


CREATE TABLE IF NOT EXISTS public.log_tags (
    log_uuid uuid NOT NULL,
    log_timestamp timestamp NOT NULL,
    tag_uuid uuid NOT NULL,
    tag_timestamp timestamp NOT NULL,
    CONSTRAINT log_tags_pkey PRIMARY KEY (log_uuid, log_timestamp, tag_uuid, tag_timestamp),

    CONSTRAINT log_tags_log_fkey FOREIGN KEY (log_uuid, log_timestamp)
        REFERENCES public.logs (log_uuid, log_timestamp)
        ON DELETE CASCADE,

    CONSTRAINT log_tags_tag_fkey FOREIGN KEY (tag_uuid, tag_timestamp)
        REFERENCES public.tags (tag_uuid, tag_timestamp)
        ON DELETE CASCADE
) PARTITION BY RANGE (log_timestamp);

--для поиска всех логов по тегу
CREATE INDEX IF NOT EXISTS idx_log_tags_tag
    ON public.log_tags (tag_uuid, tag_timestamp);

-- для джойна с logs
CREATE INDEX IF NOT EXISTS idx_log_tags_log
    ON public.log_tags (log_uuid, log_timestamp);

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
     WHILE i < days LOOP
         prefix := table_name || '_' || to_char(from_date + i * INTERVAL '1 day', 'YYYYMMDD');
         left_bound := to_char(from_date + i * INTERVAL '1 day', 'YYYY-MM-DD');
         right_bound := to_char(from_date + (i + 1) * INTERVAL '1 day', 'YYYY-MM-DD');

         IF prefix IN (
             SELECT child.relname
             FROM pg_inherits
             JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
             JOIN pg_class child ON pg_inherits.inhrelid = child.oid
             WHERE parent.relname = table_name
         ) THEN
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
         EXECUTE format(
             'CREATE INDEX IF NOT EXISTS %I ON %I (log_timestamp);',
             prefix || '_log_timestamp_idx', prefix
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
     WHILE i < days LOOP
         prefix := table_name || '_' || to_char(from_date + i * INTERVAL '1 day', 'YYYYMMDD');
         left_bound := to_char(from_date + i * INTERVAL '1 day', 'YYYY-MM-DD');
         right_bound := to_char(from_date + (i + 1) * INTERVAL '1 day', 'YYYY-MM-DD');

         IF prefix IN (
             SELECT child.relname
             FROM pg_inherits
             JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
             JOIN pg_class child ON pg_inherits.inhrelid = child.oid
             WHERE parent.relname = table_name
         ) THEN
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

         EXECUTE format(
             'CREATE INDEX IF NOT EXISTS %I ON %I (tag_timestamp);',
             prefix || '_tag_timestamp_idx', prefix
         );

         -- ВАЖНО: индекс для поиска по name и value вместе с tag_timestamp
         EXECUTE format(
             'CREATE INDEX IF NOT EXISTS %I ON %I (name, value, tag_timestamp);',
             prefix || '_name_value_timestamp_idx', prefix
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
     WHILE i < days LOOP
         prefix := table_name || '_' || to_char(from_date + i * INTERVAL '1 day', 'YYYYMMDD');
         left_bound := to_char(from_date + i * INTERVAL '1 day', 'YYYY-MM-DD');
         right_bound := to_char(from_date + (i + 1) * INTERVAL '1 day', 'YYYY-MM-DD');

         IF prefix IN (
             SELECT child.relname
             FROM pg_inherits
             JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
             JOIN pg_class child ON pg_inherits.inhrelid = child.oid
             WHERE parent.relname = table_name
         ) THEN
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

         EXECUTE format(
             'CREATE INDEX IF NOT EXISTS %I ON %I (log_uuid, log_timestamp);',
             prefix || '_log_idx', prefix
         );
         -- ИНДЕКС ДЛЯ БЫСТРОГО ПОИСКА ПО ТЕГУ (tag_uuid, tag_timestamp)
         EXECUTE format(
             'CREATE INDEX IF NOT EXISTS %I ON %I (tag_uuid, tag_timestamp);',
             prefix || '_idx_tag_uuid_timestamp', prefix
         );

         -- ИНДЕКС ДЛЯ СОЕДИНЕНИЯ С LOGS (log_uuid, log_timestamp)
         EXECUTE format(
             'CREATE INDEX IF NOT EXISTS %I ON %I (log_uuid, log_timestamp);',
             prefix || '_idx_log_uuid_timestamp', prefix
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
     PERFORM create_partitions_logs(from_date, days);
     PERFORM create_partitions_tags(from_date, days);
     PERFORM create_partitions_log_tags(from_date, days);
 END;
 $$;
