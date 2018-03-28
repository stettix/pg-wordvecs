# Word Vectors in PostgreSQL

This project contains tools that load word vector data into a
PostgreSQL instance, and uses this to perform word searches.

## Background

Word vectors are very cool - see Adrian Colyer's [The amazing power of word vectors](https://blog.acolyer.org/2016/04/21/the-amazing-power-of-word-vectors/)
for an introduction. Here we use word vectors from the [GloVe project](https://nlp.stanford.edu/projects/glove/).
                    
Recent versions of PostgreSQL come with an extension, [cube](https://www.postgresql.org/docs/current/static/cube.html), that provides a column type for cubes and points of 
arbitrary dimensions, and crucially, implements indexing and distance functions for these entities, and a 'K-nearest neighbors' algorithm
for efficiently finding similar values.

Put the two together, and we have a way of running queries on how words relate to each other. 
This code in this project explores a couple of such use cases. 

## Loading word vectors into PostgreSQL

The `LoadVectors` command line tool reads word vectors from a given file and writes them to PostgreSQL using 
provided connection details. You can run this against a stock version of PostgreSQL with a sufficiently recent version.
provided that the [cube extension](https://www.postgresql.org/docs/current/static/cube.html) is available (see details below).

The tool will create the necessary table and index if it doesn't exist already.

Note that the 'cube' extension has a limit of 100 on the size of vectors by default. The GloVe dataset with word vectors
of size 50 works well.

## Querying word vectors in PostgreSQL

The `QueryExamples` app contains some of example queries that can be run on the loaded word vectors.
 
### Finding similar words

This includes finding the list of most similar words to a word. For example, finding the words most similar to 'car'
returns car, truck, cars, vehicle, driver, driving, bus etc. The words most similar to 'knitting' are: 'sewing', 'dyeing',
'embroidery', 'weaving' and so on.

### Navigating word relationships

It also runs code to use existing word relationships to find equivalent relationships. This basically answers queries
of the form "X is to Y as Z is to ...?". Some example results returned:

 * man is to woman as king is to: *queen*
 * man is to woman as sir is to: *elizabeth*
 * man is to woman as brother is to: *daughter*
 * norway is to oslo as france is to: *paris*
 * strong is to stronger as heavy is to: *heavier*
 * cat is to kitten as dog is to: *puppy*

As we see, this works fairly well, even if some of these results don't make sense!

## Discussion

### Query expressivity

To find the most similar words, you can use a a very simple SQL query like this:

```
  select word from <table>
    order by (select vector from <table> where word = $word) <-> vector limit 10
```

The `<->` operator is the euclidian distance operator.

So this is nice and easy.

However, to answer queries along the line of "X is to Y what Z is to ...", we need to find the distance between two
points (X and Y), and add this to the vector for word Z. However, the cube extension doesn't provide addition and subtraction
operation on cubes, nor does PostgreSQL come with such operators for array types. Hence I implemented this in client-side
code instead. This is a bit disappointing and limits the use cases for this setup.

### Distance metrics

The [kNN implementation](https://www.postgresql.org/message-id/9E07E159-E405-41E2-9889-A04F534FC257@gmail.com) we're using here
supports various distance metrics: euclidian, taxicab (i.e. manhattan) and chebyshev distance. I'm using euclidian distances
in these examples.

When comparing vectors, cosine-distance is often a useful metric. This can not be implemented using a kNN algorithm so is not
available to us when using PostgreSQL in this way. We try to reduce the impact of this by normalising the word vectors on 
insertion, i.e. ensuring they're all of the same magnitude. No attempt has been made as of yet to measure how much of an
impact this has.

### Performance

So far I've only tried running this against a local single-node PostgreSQL instance on my (lightweight!) laptop. For word
similarity queries I saw quite variable results depending on the word, from ~60-70 ms to over 400 ms. No attempt has been
made to tune this or figure out why the results vary so much, so take these numbers with a very large pinch of salt.

This is still much faster that the same query when no spatial index exists, i.e. doing a full table scan - this would
typically take 3 to 4 seconds. So even in the worst case scenarios seen, it's an order of magnitude faster.

### So how useful is this really?

This was just an experiement to see how well the `cube` extension works for vector similarity operations. From our results
it seems to work pretty well. That said, with datasets as small as the WordVectors we've used here, you're probably better
off storing these in memory either using a brute force table scan, or an in-memory data structure like an R-tree or similar.

That said, being able to keep and update vector data like this in a database and use all the tools that come with it 
is potentially valuable.

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

The example code is implemented using [doobie](https://github.com/tpolecat/doobie) for database access, which is fab.

It also uses [breeze](https://github.com/scalanlp/breeze) for the very small amount of linear algebra that it performs.
