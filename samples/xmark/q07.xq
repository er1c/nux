(: Q7. Aggregation and arithmetics. How many pieces of prose are in our database? :)

for $p in /site
return count($p//description) + count($p//annotation) + count($p//email)
