(: Find all book chapters containing the phrase "one of the best known lists of heuristics is Ten Usability Heuristics" :)

declare namespace lucene = "java:nux.xom.pool.FullTextUtil";
for $book in /books/book
let $chap := $book//chapter[lucene:match(., 
   '"one of the best known lists of heuristics is Ten Usability Heuristics"') > 0.0]
where count($chap) > 0
return $book
