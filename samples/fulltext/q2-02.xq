(: Find all book subjects containing the phrase "usability testing" :)

declare namespace lucene = "java:nux.xom.pool.FullTextUtil";
/books/book/metadata/subjects/subject[lucene:match(., '"usability testing"') > 0.0]