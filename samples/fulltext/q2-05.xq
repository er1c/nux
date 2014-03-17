(: Find all books with the phrase "usability testing" in some subject :)

declare namespace lucene = "java:nux.xom.pool.FullTextUtil";
for $book in /books/book
where some $subject in $book//subject satisfies lucene:match($subject, '"usability testing"') > 0.0
return $book/metadata/(title|author)
