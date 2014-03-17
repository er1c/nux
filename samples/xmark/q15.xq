(: Q15. Loooong path expression. Print the keywords in emphasis in annotations of closed auctions. :)

for $a in /site/closed_auctions/closed_auction/annotation/
          description/parlist/listitem/parlist/listitem/text/emph/keyword/text()
return <text> {$a} </text>

