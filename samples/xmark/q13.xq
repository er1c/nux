(: Q13. Long path expression. List the names of items registered in Australia along with their descriptions. :)

for $i in /site/regions/australia/item
return <item name="{$i/name/text()}"> {$i/description} </item>

