-- grab the first schema from schema.grow using pattern match like listSchemas() in grow's common.sh
\set schema `awk '$0~/^[a-z]/ {print;exit}' "$grow_dir/GROW/APPLICATION/schema.grow"`

select :"schema".tenant_init(test.tid());
select :"schema".tenant_init(test.tid());
select :"schema".tenant_init(test.tid());
