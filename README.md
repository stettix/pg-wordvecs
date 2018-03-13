# Word Vectors in PostgreSQL

This project contains tools that load word vector data into a
PostgreSQL instance, and uses this to perform word searches.

Details **TBD**

## Dependencies

This code needs access to a running PostgreSQL instance.

This instance needs to have the [cube](https://www.postgresql.org/docs/current/static/cube.html) extension installed. This extension comes from the `postgresql-contrib` package
which needs to be installed. Then the extension can be loaded with the command:

```
> CREATE EXTENSION cube;
```

You can check that the extension is available by running this query:

```
select cube_union('(0,5,2),(2,3,1)', '0');
```
