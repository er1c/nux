(: Find all books with word "identify" in book introductions and part introductions :)

declare namespace lucene = "java:nux.xom.pool.FullTextUtil";
for $book in /books/book
let $bi := $book/content/introduction[some $p in ./p satisfies lucene:match($p, "identif*") > 0.0]
let $pi := $book/content/part/introduction[some $p in ./p satisfies lucene:match($p, "identif*") > 0.0]
where count($bi)>0 and count($pi)>0
return $book
