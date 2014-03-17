(: Find all book titles which start with "improving" followed within 2 words by "usability" :)

declare namespace lucene = "java:nux.xom.pool.FullTextUtil";
for $book in /books/book
let $title := $book/metadata/title[lucene:match(., '"improving usability"~2') > 0.0]
where count($title) > 0
return $title
