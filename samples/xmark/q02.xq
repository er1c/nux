(: Q2. Element construction. Return the initial increases of all open auctions. :)

for $b in /site/open_auctions/open_auction
return <increase> {$b/bidder[1]/increase/text()} </increase>

