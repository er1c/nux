(: Q6. More aggregation. How many items are listed on all continents? :)

for    $b in /site/regions
return count ($b//item)

