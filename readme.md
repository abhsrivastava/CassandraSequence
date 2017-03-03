# Cassandra Sequence
Attempt to implement a auto incrementing unique ID on top of cassandra.
Couldn't use counter because needed to backfill data and preserve those IDs.

Need Two tables in cassandra
 
*  create table list_id_tracker(list_name text primary key, id int);
*  create table my_list(id int primary key, list_data list<text>)
 