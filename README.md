# OntoLogistics

Simulateur de réseau logistique qui combine quatre approches d'ingénierie logicielle dans un seul projet : un DSL maison compilé à la volée, un système multi-agents qui négocie tout seul, un moteur de raisonnement ontologique, et un algorithme de plus court chemin.

L'idée de départ : plutôt que de coder en dur "si un produit fragile dépasse 25°C, alors il faut un camion réfrigéré", ce genre de règle est déclaré dans une ontologie et déduit par un moteur d'inférence. Plutôt que d'écrire un formulaire pour créer des entrepôts et des camions un par un, on décrit tout un réseau dans un petit langage texte, et un compilateur maison le transforme en objets Java. Et quand un camion tombe en panne, ce n'est pas une fonction qui réassigne bêtement le premier camion libre : un agent coordinateur lance un appel d'offres, les camions disponibles répondent, et un agent "capital" vérifie que le remplacement rentre dans le budget avant d'accepter.

## Sommaire

- [Les quatre piliers](#les-quatre-piliers)
- [Stack](#stack)
- [Structure du projet](#structure-du-projet)
- [Lancer le projet en local](#lancer-le-projet-en-local)
- [API](#api)
- [Le DSL FlexNet](#le-dsl-flexnet)
- [Ontologie et règles](#ontologie-et-règles)
- [Le système multi-agents](#le-système-multi-agents)
- [Routage](#routage)
- [Ce qui manque encore](#ce-qui-manque-encore)

## Les quatre piliers

**Ingénierie Dirigée par les Modèles (IDM).** Un fichier `.flexnet` décrit un réseau (entrepôts, camions, commandes) en texte. Un lexer/parser maison le transforme en arbre syntaxique, un validateur sémantique vérifie que les références entre déclarations tiennent la route, puis un générateur produit du code Java qui peut être compilé et déployé directement en base. Le pipeline complet est dans `com.flexchain.idm`.

**Système Multi-Agents (SMA).** Les agents communiquent par messages typés (`ACLMessage` + `Performative`), dans l'esprit de FIPA-ACL. Quand un camion tombe en panne, le coordinateur envoie un appel d'offres, les camions répondent avec une proposition, et l'agent capital valide le coût avant d'accepter — un vrai protocole Contract Net, pas une boucle qui prend le premier disponible.

**Ontologie.** Le vocabulaire métier est déclaré en OWL/RDF (`flexchain-ontology.ttl`), et les règles de décision en Jena Rules (`flexchain-rules.rules`). Deux règles sont chaînées : une commande fragile trop exposée à la chaleur nécessite un changement de camion, et si ce changement est nécessaire pour une commande fragile, le camion de remplacement doit être réfrigéré. La seconde règle consomme la conclusion de la première — c'est de l'inférence, pas une condition Java.

**Routage.** Le réseau routier est un graphe pondéré, parcouru par un Dijkstra classique pour trouver le plus court chemin entre deux points.

## Stack

Java 17, Spring Boot 3.5.4 (Web, Data JPA, Validation, Scheduling), PostgreSQL, Apache Jena pour le RDF/OWL, Maven, Docker. Le frontend est du HTML/CSS/Vanilla.js.

## Structure du projet

```
src/main/java/com/flexchain
├── agent/           agents SMA (coordinateur, camion, capital) + protocole ACL
├── config/          CORS, chargement des données initiales
├── controller/      endpoints REST métier (camions, commandes, entrepôts, dashboard...)
├── entity/          entités JPA
├── idm/             pipeline DSL : lexer, parser, validateur, générateur de code
├── ontology/        vocabulaire OWL + service de raisonnement Jena
├── orchestrator/    déclenchement autonome des incidents
├── repository/      Spring Data JPA
├── routing/         graphe routier + Dijkstra
└── service/         logique métier

src/main/resources
├── application.yml
├── flexnet-samples/ exemples de réseaux .flexnet
├── ontology/        flexchain-ontology.ttl + flexchain-rules.rules
└── static/          dashboard et éditeur FlexNet (frontend statique)
```

Rien de plus caché derrière : chaque pilier a son propre package, pas de couche transverse qui mélange tout.

## Lancer le projet en local

Il faut Java 17, Maven, et une base PostgreSQL accessible (locale ou via Docker).

```bash
git clone https://github.com/Kuroi-Kage/ontology-driven-logistics.git
cd ontology-driven-logistics
createdb flexchain
mvn spring-boot:run
```

Ça démarre sur `localhost:8080`. Le dashboard est à la racine, l'éditeur FlexNet sur `/idm.html`.

Avec Docker, si tu préfères ne pas installer Postgres en local :

```bash
docker build -t flexchain .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/flexchain \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=postgres \
  flexchain
```

## API

Ressources de base :

```
GET    /trucks              GET    /trucks/{id}
POST   /trucks               PUT    /trucks/{id}
GET    /orders               POST   /orders
GET    /warehouses           POST   /warehouses          GET /warehouses/{id}
```

Incidents et orchestration :

```
GET    /incidents                        POST /incidents
GET    /incidents/events/after/{id}      GET  /incidents/events/latest-id
POST   /simulation/breakdown/{truckId}   déclenche une panne et lance la négociation SMA
GET    /orchestrator/status              POST /orchestrator/toggle
POST   /orchestrator/probability
POST   /api/capital/decide
```

Ontologie :

```
POST   /ontology/evaluate            évaluation ad hoc, sans toucher la base
GET    /ontology/evaluate/{orderId}  évaluation d'une commande existante
```

IDM :

```
GET    /idm/sample     exemple de réseau .flexnet
POST   /idm/compile    compile en Java, sans déployer
POST   /idm/deploy     compile et persiste en base
```

Routage et dashboard :

```
GET    /routes
GET    /dashboard/overview
```

## Le DSL FlexNet

```flexnet
network "DemoNetwork" {

    warehouse W1 {
        name: "Warehouse A"
        location: "Antananarivo"
        latitude: -18.8792
        longitude: 47.5079
    }

    truck T1 {
        code: "TRUCK-01"
        driver: "Jean"
        capacity: 12.0
        status: AVAILABLE
    }

    order O1 {
        reference: "ORD-001"
        destination: "Toamasina"
        warehouse: W1
        status: PENDING
    }
}
```

Ce texte passe par un lexer, un parser qui construit un `NetworkModel`, un validateur sémantique qui vérifie par exemple qu'une commande référence bien un entrepôt déclaré plus haut, et enfin un générateur qui produit du code Java. `/idm/compile` renvoie juste le code généré pour inspection, `/idm/deploy` va jusqu'à persister les entités en base. La grammaire complète (EBNF) est dans `GRAMMAR.md`.

## Ontologie et règles

L'ontologie déclare les classes du domaine — une commande, un camion, et un camion réfrigéré comme sous-classe de camion. Les règles, elles, vivent dans un fichier séparé et raisonnent sur des faits injectés à l'exécution :

```
[fragileHighTemperatureRule:
    (?order flex:isFragile true)
    (?order flex:currentTemperatureCelsius ?t)
    greaterThan(?t, 25)
    -> (?order flex:requiresTruckChange true)]

[refrigerationNeededRule:
    (?order flex:requiresTruckChange true)
    (?order flex:isFragile true)
    -> (?order flex:requiresRefrigeratedTruck true)]
```

`OntologyReasoningService` prend les données d'une commande, les injecte comme faits, lance le moteur Jena, et récupère ce qui a été déduit. C'est ce résultat qui guide le SMA quand il cherche un camion de remplacement.

## Le système multi-agents

Quatre agents : `CoordinatorAgent` pilote la négociation, `TruckAgent` représente la flotte et répond aux appels d'offres, `CapitalAgentService` tient un budget et refuse ce qui ne rentre pas dedans, `OrderAgent` porte les contraintes d'une commande. Les échanges sont des `ACLMessage` avec un `Performative` (INFORM, CFP, PROPOSE, ACCEPT, REJECT), et chaque négociation est journalisée pour être rejouée côté dashboard.

L'`AutoIncidentOrchestrator` tourne en tâche planifiée et déclenche des pannes aléatoires tout seul, à intervalle et probabilité configurables. Il repasse par exactement le même chemin de code que le bouton "simuler une panne" du dashboard, donc pas de logique dupliquée entre le mode automatique et le mode manuel.

## Routage

Graphe pondéré (`RoadNetwork`, `RoadNode`, `RoadEdge`), Dijkstra pour le plus court chemin, distances calculées avec la formule de Haversine dans `GeoUtils`.

## Ce qui manque encore

Pas d'authentification sur les routes sensibles (`/idm/deploy`, `/orchestrator/*`). Le schéma de base tourne encore avec `ddl-auto: update`, ce qui est pratique en dev mais pas fait pour durer — une vraie migration Flyway serait plus propre. Pas de tests pour l'instant, ni sur le parser du DSL, ni sur les règles Jena, ni sur la négociation SMA. L'historique des négociations n'est pas persisté au-delà de la session en cours.
