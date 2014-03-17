(: Q18. Function call. Convert the currency of the reserve of all open auctions to :)
(:     another currency. :)

declare function local:convert ($v)
{
   2.20371 * $v    (: convert Dfl to Euro :)
};

for    $i in /site/open_auctions/open_auction
return local:convert($i/reserve)


