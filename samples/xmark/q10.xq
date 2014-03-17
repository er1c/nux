(: Q10. Grouping. List all persons according to their interest; :)
(:      use French markup in the result. :)

for $i in distinct-values(
          /site/people/person/profile/interest/@category)
let $p := for    $t in /site/people/person
          where  $t/profile/interest/@category = $i
            return <personne>
                <statistiques>
                        <sexe> {$t/gender/text()} </sexe>,
                        <age> {$t/age/text()} </age>,
                        <education> {$t/education/text()}</education>,
                        <revenu> {$t/income/text()} </revenu>
                </statistiques>,
                <coordonnees>
                        <nom> {$t/name/text()} </nom>,
                        <rue> {$t/street/text()} </rue>,
                        <ville> {$t/city/text()} </ville>,
                        <pays> {$t/country/text()} </pays>,
                        <reseau>
                                <courrier> {$t/email/text()} </courrier>,
                                <pagePerso> {$t/homepage/text()}</pagePerso>
                        </reseau>,
                </coordonnees>
                <cartePaiement> {$t/creditcard/text()}</cartePaiement>    
              </personne>
return <categorie>
        <id> {$i} </id>,
        {$p}
      </categorie>

