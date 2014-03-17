(: Q16. More loooong path expression. Return the IDs of those auctions :)
(:      that have one or more keywords in emphasis. (cf. Q15) :)

for $a in /site/closed_auctions/closed_auction
where not( empty ($a/annotation/description/parlist/listitem/parlist/
                     listitem/text/emph/keyword/text()))
return <person id="{$a/seller/@person}" />

