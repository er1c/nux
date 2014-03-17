(: Q17. Emptiness. Which persons don't have a homepage? :)

for    $p in /site/people/person
where  empty($p/homepage/text())
return <person name="{$p/name/text()}"/>

