(: Q14. String operation. Return the names of all items whose description contains the word `gold'. :)

for $i in /site//item
where contains ($i/description,"gold")
return $i/name/text()

