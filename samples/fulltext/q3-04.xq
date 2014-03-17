(: Find all book text with the phrase "usability testing once the problems" :)

declare namespace lucene = "java:nux.xom.pool.FullTextUtil";
for $book in /books/book
let $cont := $book//content[lucene:match(., '"usability testing once the problems"') > 0.0]
where count($cont)>0
return $book
