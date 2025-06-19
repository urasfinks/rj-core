-- Table: public.logger

-- DROP TABLE IF EXISTS public.logger;

CREATE TABLE IF NOT EXISTS public.logger
(
    id bigint NOT NULL DEFAULT nextval('logger_id_seq'::regclass),
    date_add timestamp with time zone NOT NULL DEFAULT now(),
    type character varying(255) COLLATE pg_catalog."default" NOT NULL,
    correlation character varying(255) COLLATE pg_catalog."default" NOT NULL,
    host character varying(255) COLLATE pg_catalog."default",
    ext_index character varying(255) COLLATE pg_catalog."default",
    data text COLLATE pg_catalog."default",
    CONSTRAINT logger_pkey PRIMARY KEY (id, date_add)
) PARTITION BY RANGE (date_add);

ALTER TABLE IF EXISTS public.logger
    OWNER to postgres;
-- Index: idx_01

-- DROP INDEX IF EXISTS public.idx_01;

CREATE INDEX IF NOT EXISTS idx_01
    ON public.logger USING btree
    (date_add ASC NULLS LAST)
;
-- Index: idx_02

-- DROP INDEX IF EXISTS public.idx_02;

CREATE INDEX IF NOT EXISTS idx_02
    ON public.logger USING btree
    (ext_index COLLATE pg_catalog."default" ASC NULLS LAST)
;