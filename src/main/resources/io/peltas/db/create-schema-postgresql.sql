CREATE TABLE peltas_timestamp
(
  access timestamp without time zone NOT NULL,
  application_name character varying(255) NOT NULL,
  ref character varying(255) NOT NULL,
  CONSTRAINT peltas_timestamp_pk PRIMARY KEY (application_name),
  CONSTRAINT peltas_timestamp_unique UNIQUE (ref, application_name)
)
WITH (
  OIDS=FALSE
);