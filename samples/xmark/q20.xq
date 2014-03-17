(: Q20. Back to group by. Group customers by their :)
(:      income and output the cardinality of each group. :)

<result>
 <preferred>
  {count (/site/people/person/profile[@income >= 100000])}
 </preferred>,
 <standard>
  {count (/site/people/person/profile[@income < 100000
                                                        and @income >= 30000])}
 </standard>,
 <challenge> 
  {count (/site/people/person/profile[@income < 30000])}
 </challenge>,
 <na>
  {count (for    $p in /site/people/person
         where  empty($p/@income)
         return $p)}
 </na>
</result>
