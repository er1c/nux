(: Find all books if any one contains the word "mouse" :)

declare namespace lucene = "java:nux.xom.pool.FullTextUtil";
for $book in /books/book
where lucene:match($book, "Mouse") > 0.0
return $book
