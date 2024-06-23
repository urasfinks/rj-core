CREATE OR REPLACE PROCEDURE public.create_partition(start_p timestamp, n int4, plus interval)
    LANGUAGE plpgsql AS
$$
DECLARE
	_sql text;
	_partition_name text;
	_counter int4;
	_cur timestamp;
	_rec record;
BEGIN
	_cur := start_p;
	FOR _counter IN 1..5 LOOP
		_partition_name := 'logger_' || _cur;
		IF NOT EXISTS (SELECT FROM pg_catalog.pg_tables WHERE  schemaname = 'public' AND tablename = _partition_name) THEN
			_sql := format($i$

				CREATE TABLE public."%3$s" (LIKE public.logger INCLUDING ALL);
				ALTER TABLE public."%3$s" ADD CONSTRAINT "checker_%3$s" CHECK (date_add >= %1$L::timestamp AND date_add < %2$L::timestamp);
				ALTER TABLE public.logger ATTACH PARTITION public."%3$s" FOR VALUES FROM (%1$L) TO (%2$L);
				ALTER TABLE public."%3$s" DROP CONSTRAINT "checker_%3$s";

			$i$, _cur, _cur + plus, _partition_name);

			RAISE NOTICE '%', _sql;
			EXECUTE _sql;

		END IF;
		_cur := _cur + plus;
	END LOOP;
	FOR _rec IN (
		SELECT tablename FROM pg_catalog.pg_tables
			WHERE  schemaname = 'public'
			AND tablename like '%logger_%'
			AND tablename < ('logger_' || start_p)
			ORDER BY tablename DESC
			OFFSET n
	) LOOP
		_sql := format($i$

			TRUNCATE TABLE  public."%1$s";
			ALTER TABLE public."logger" DETACH PARTITION public."%1$s";
			DROP TABLE public."%1$s";

			$i$, _rec.tablename);

		RAISE NOTICE '%', _sql;
		EXECUTE _sql;

	END LOOP;
END;
$$;
