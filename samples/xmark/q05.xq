(: Q5. Aggregation. How many sold items cost more than 40? :)

count(for $i in /site/closed_auctions/closed_auction
        where  $i/price/text() >= 40 
        return $i/price)

