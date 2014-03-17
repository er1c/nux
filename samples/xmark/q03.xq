(: Q3. Complex selection. Return the IDs of all open auctions whose current :)
(:     increase is at least twice as high as the initial increase. :)

for    $b in /site/open_auctions/open_auction
where  $b/bidder[1]/increase/text() * 2 <= $b/bidder[last()]/increase/text()
return <increase first="{$b/bidder[1]/increase/text()}"
                 last="{$b/bidder[last()]/increase/text()}"/>

