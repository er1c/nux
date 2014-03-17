(: Find all books with "usability tests" in book or chapter titles :)

declare namespace lucene = "java:nux.xom.pool.FullTextUtil";
for $book in /books/book
let $title := ($book/metadata/title[lucene:match(., '"usability tests"') > 0.0] 
   , $book/content/part/chapter/title[lucene:match(., '"usability tests"') > 0.0])
where count($title) > 0
return $book
