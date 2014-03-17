(: Find all book titles containing the word "usability" :)

declare namespace lucene = "java:nux.xom.pool.FullTextUtil";
/books/book/metadata/title[lucene:match(., "usability") > 0.0]