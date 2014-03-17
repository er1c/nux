(: Find all part introductions containing the word "prototypes" :)

declare namespace lucene = "java:nux.xom.pool.FullTextUtil";
for $book in /books/book
let $intro := $book/content/part/introduction[lucene:match(., "prototypes") > 0.0]
where count($intro)>0
return $book
