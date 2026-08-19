# Grammaire du DSL FlexNet (notation EBNF)
# Fichier source : *.flexnet
# Ce langage textuel permet de decrire un reseau logistique (entrepots,
# camions, commandes) de maniere independante de toute technologie.
# Il constitue le modele source (PIM) du pipeline IDM FlexChain :
#   texte .flexnet --[Lexer]--> tokens --[Parser]--> AST (NetworkModel)
#   --[SemanticValidator]--> AST valide --[JavaDataLoaderGenerator]--> code Java

network        ::= "network" STRING "{" declaration* "}" ;

declaration    ::= warehouseDecl | truckDecl | orderDecl ;

warehouseDecl  ::= "warehouse" IDENT "{" warehouseProp* "}" ;
warehouseProp  ::= "name" ":" STRING
                  | "location" ":" STRING
                  | "latitude" ":" NUMBER
                  | "longitude" ":" NUMBER ;

truckDecl      ::= "truck" IDENT "{" truckProp* "}" ;
truckProp      ::= "code" ":" STRING
                  | "driver" ":" STRING
                  | "capacity" ":" NUMBER
                  | "status" ":" truckStatus ;
truckStatus    ::= "AVAILABLE" | "BUSY" | "BROKEN" ;

orderDecl      ::= "order" IDENT "{" orderProp* "}" ;
orderProp      ::= "reference" ":" STRING
                  | "destination" ":" STRING
                  | "warehouse" ":" IDENT        (* reference vers un warehouseDecl *)
                  | "status" ":" orderStatus ;
orderStatus    ::= "PENDING" | "IN_PROGRESS" | "DELIVERED" ;

(* Terminaux *)
IDENT   ::= LETTER (LETTER | DIGIT | "_")* ;
STRING  ::= '"' <any character except '"'>* '"' ;
NUMBER  ::= DIGIT+ ("." DIGIT+)? ;

(* Commentaires : "// ..." jusqu'a fin de ligne, ignores par le lexer *)

# Contraintes semantiques (verifiees par SemanticValidator, hors grammaire) :
#  - identifiants warehouse/truck/order uniques dans leur propre espace de noms
#  - truck.capacity > 0
#  - truck.code non vide et unique
#  - order.warehouse doit referencer un identifiant warehouse declare plus haut
#    (integrite referentielle inter-declarations)
#  - tous les champs listes ci-dessus sont obligatoires
