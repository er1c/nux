(: Q19. Sort by. Give an alphabetically ordered list of all :)
(:      items along with their location. :)

for $b in /site/regions//item
let $k := $b/name/text()
order by $k
return <item name="{$k}"> {$b/location/text()} </item>

