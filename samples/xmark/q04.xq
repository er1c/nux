(: Q4. Document order. List the reserves of those open auctions where a :)
(:     certain person issued a bid before another person. :)

for    $b in /site/open_auctions/open_auction
where  $b/bidder/personref[@person="person18829"] <<
       $b/bidder/personref[@person="person10487"]
return <history> $b/reserve/text() </history>

