(: Q11. More group by. For each person, list the number of items currently on sale whose :)
(:      price does not exceed 0.02% of the person's income. :)

for $p in /site/people/person
let $l := for $i in /site/open_auctions/open_auction/initial
          where $p/profile/@income > (5000 * $i/text())
          return $i
return <items name="{$p/name/text()}"> {count ($l)} </items>

