-- validate functions as grow creates with 'set check_function_bodies = false;'

\set ECHO errors
\t

set search_path = '';

SELECT E'SELECT \'Validating function: ' || p.oid::regprocedure || E'\';',
       'SELECT ' || pl.proname || '(' || p.oid || ');'
FROM pg_catalog.pg_proc p
     LEFT JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace
     LEFT JOIN pg_catalog.pg_language l ON l.oid = p.prolang
     LEFT JOIN pg_catalog.pg_proc pl ON pl.oid = l.lanvalidator
WHERE n.nspname <> 'pg_catalog'
  AND n.nspname <> 'information_schema' \gexec
