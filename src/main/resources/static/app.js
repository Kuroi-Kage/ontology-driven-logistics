/**
 * FlexChain — Terrain SMA / IDM
 * Frontend statique servi par Spring Boot.
 */

"use strict";

/* ================= CONFIGURATION ================= */

const CONFIG = {
    apiBase: ""
};

/* ================= ETAT ================= */

const state = {
    trucks: [],
    warehouses: [],
    selectedTruck: null,

    /*
     * Ids des camions actuellement en cours de deplacement anime
     * (panne/remplacement ou deambulation ambiante). Permet a
     * renderTrucks() de ne pas casser une transition CSS en cours en
     * recreant l'element au mauvais moment.
     */
    animatingTruckIds: new Set(),

    /*
     * Tant qu'aucun modele IDM n'a ete compile avec succes, aucune
     * simulation (panne manuelle, orchestrateur auto, deambulation
     * ambiante) ne doit s'executer. Passe a true a la fin d'un
     * compileIdm() reussi.
     */
    idmCompiled: false
};

const els = {};

/* ================= INITIALISATION ================= */

document.addEventListener("DOMContentLoaded", init);

async function init() {
    cacheElements();
    bindEvents();

    setSimulationLocked(true);

    await loadData();
    await loadIdmSample();
    await initOrchestrator();

    startAmbientWander();
}

/*
 * Verrouille/deverrouille les simulations (panne manuelle,
 * orchestrateur auto, deambulation ambiante). Tant que c'est
 * verrouille : boutons desactives, deambulation et incidents auto
 * ignores en silence (mais toujours suivis, pour ne pas rejouer un
 * historique en rafale une fois debloque).
 */
function setSimulationLocked(locked) {

    state.idmCompiled = !locked;

    if (els.breakdownBtn) {
        els.breakdownBtn.disabled = locked;
        els.breakdownBtn.title = locked
            ? "Compile d'abord ton modèle IDM pour activer les simulations."
            : "";
    }

    if (els.orchestratorToggleBtn) {
        els.orchestratorToggleBtn.disabled = locked;
        els.orchestratorToggleBtn.title = locked
            ? "Compile d'abord ton modèle IDM pour activer les simulations."
            : "";
    }
}

function cacheElements() {
    els.field = document.querySelector(".sma-field");
    els.smaMessage = document.getElementById("smaMessage");
    els.negotiationLog = document.getElementById("negotiationLog");

    els.idmSource = document.getElementById("idmSource");
    els.idmInfo = document.getElementById("idmInfo");
    els.generatedJava = document.getElementById("generatedJava");

    els.addTruckBtn = document.getElementById("addTruckBtn");
    els.addWarehouseBtn = document.getElementById("addWarehouseBtn");
    els.breakdownBtn = document.getElementById("breakdownBtn");

    els.orchestratorToggleBtn = document.getElementById("orchestratorToggleBtn");
    els.orchestratorStatusEl = document.getElementById("orchestratorStatus");

    els.loadSampleBtn = document.getElementById("loadSampleBtn");
    els.compileBtn = document.getElementById("compileBtn");

    els.modal = document.getElementById("modal");
    els.modalForm = document.getElementById("modalForm");
    els.modalTitle = document.getElementById("modalTitle");
    els.modalFields = document.getElementById("modalFields");
    els.modalError = document.getElementById("modalError");
    els.modalCancel = document.getElementById("modalCancel");
}

function bindEvents() {
    els.addTruckBtn?.addEventListener("click", openAddTruckModal);
    els.addWarehouseBtn?.addEventListener("click", openAddWarehouseModal);
    els.breakdownBtn?.addEventListener("click", simulateBreakdown);
    els.orchestratorToggleBtn?.addEventListener("click", toggleOrchestrator);

    els.loadSampleBtn?.addEventListener("click", loadIdmSample);
    els.compileBtn?.addEventListener("click", compileIdm);

    els.modalCancel?.addEventListener("click", closeModal);

    els.modal?.addEventListener("click", event => {
        if (event.target === els.modal) {
            closeModal();
        }
    });
}

/* ================= API ================= */

async function apiFetch(path, options) {

    const response = await fetch(
        `${CONFIG.apiBase}${path}`,
        options
    );

    if (!response.ok) {

        const message =
            await response.text().catch(() => "");

        throw new Error(
            message || `Erreur HTTP ${response.status}`
        );
    }

    const contentType =
        response.headers.get("content-type") || "";

    return contentType.includes("application/json")
        ? response.json()
        : response.text();
}

async function withLoading(button, action) {

    if (!button) {
        return action();
    }

    const originalText = button.textContent;

    button.disabled = true;

    try {
        return await action();
    } finally {
        button.disabled = false;
        button.textContent = originalText;
    }
}

/* ================= COORDONNEES → TERRAIN ================= */

/*
 * Transforme les coordonnées géographiques de Madagascar
 * en position (%) dans le terrain SMA.
 *
 * Les coordonnées viennent directement de PostgreSQL.
 */

function geoToField(lat, lon) {

    if (
        lat === null ||
        lat === undefined ||
        lon === null ||
        lon === undefined
    ) {
        return {
            left: 50,
            top: 50
        };
    }

    const minLat = -25.5;
    const maxLat = -12.0;

    const minLon = 43.0;
    const maxLon = 50.0;

    const left =
        ((lon - minLon) /
            (maxLon - minLon)) * 100;

    const top =
        ((maxLat - lat) /
            (maxLat - minLat)) * 100;

    return {
        left: Math.max(3, Math.min(97, left)),
        top: Math.max(5, Math.min(95, top))
    };
}

/* ================= ROUTES (backend : reseau routier + Dijkstra) ================= */

/*
 * Distance a vol d'oiseau (km), utilisee cote client uniquement pour
 * trouver l'entrepot le plus proche d'un camion. Le vrai calcul
 * d'itineraire (Dijkstra sur le reseau routier) est fait par le backend
 * via /routes.
 */

function haversineKm(lat1, lon1, lat2, lon2) {

    const toRad = value => (value * Math.PI) / 180;

    const R = 6371;

    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);

    const a =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(toRad(lat1)) *
        Math.cos(toRad(lat2)) *
        Math.sin(dLon / 2) ** 2;

    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function nearestWarehouse(lat, lon) {

    if (!state.warehouses.length) return null;

    return state.warehouses.reduce((closest, warehouse) => {

        const distance = haversineKm(
            lat, lon,
            warehouse.latitude, warehouse.longitude
        );

        return (!closest || distance < closest.distance)
            ? { warehouse, distance }
            : closest;

    }, null)?.warehouse;
}

async function fetchRoute(fromLat, fromLon, toLat, toLon) {

    const params = new URLSearchParams({
        fromLat, fromLon, toLat, toLon
    });

    return apiFetch(`/routes?${params.toString()}`);
}

function clearRoute() {

    document
        .querySelectorAll(".route-overlay")
        .forEach(element => element.remove());
}

/*
 * Dessine le trajet renvoye par le backend (waypoints en lat/lon,
 * convertis en % via geoToField pour se superposer au terrain).
 */

function renderRoute(waypoints) {

    /*
     * Le trajet (waypoints) reste calcule et utilise pour animer les
     * deplacements segment par segment (voir animateElementAlongRoute) ;
     * seule la ligne visuelle du trajet sur le terrain est desactivee,
     * a la demande, pour un rendu plus epure.
     */

    clearRoute();
}

/*
 * Calcule et affiche l'itineraire reel (reseau routier interne) entre
 * l'entrepot le plus proche et le camion selectionne.
 */

async function showRouteToTruck(truck) {

    if (truck.latitude == null || truck.longitude == null) {
        clearRoute();
        return;
    }

    const warehouse = nearestWarehouse(
        truck.latitude,
        truck.longitude
    );

    if (!warehouse) {
        clearRoute();
        return;
    }

    try {

        const route = await fetchRoute(
            warehouse.latitude,
            warehouse.longitude,
            truck.latitude,
            truck.longitude
        );

        renderRoute(route.waypoints);

        setSmaMessage(
            `${truck.code} sélectionné · route depuis ${warehouse.name} : ` +
            `${route.distanceKm} km (${(route.roadsUsed || []).join(" → ") || "trajet direct"})`
        );

    } catch (error) {

        console.error(
            "Impossible de calculer la route :",
            error
        );
    }
}

/* ================= ANIMATION LE LONG D'UN TRAJET ================= */

/*
 * Point de base du Coordinator : la capitale (Antananarivo), qui est
 * aussi le noeud central du reseau routier. Le Coordinator "part du
 * QG" pour rejoindre un camion en panne.
 */

const COORDINATOR_BASE = {
    latitude: -18.8792,
    longitude: 47.5079
};

/*
 * Deplace un element DOM segment par segment le long des waypoints d'un
 * vrai trajet (renvoyes par /routes), au lieu d'un saut direct en ligne
 * droite. La duree de chaque segment est proportionnelle a sa distance
 * reelle (rythme de demo, pas une vitesse realiste).
 */

function animateElementAlongRoute(element, waypoints) {

    return new Promise(resolve => {

        if (!element || !waypoints || waypoints.length < 2) {
            resolve();
            return;
        }

        let index = 0;

        function nextSegment() {

            if (index >= waypoints.length - 1) {
                resolve();
                return;
            }

            const from = waypoints[index];
            const to = waypoints[index + 1];

            const distanceKm = haversineKm(
                from.latitude, from.longitude,
                to.latitude, to.longitude
            );

            const durationSec = Math.min(
                2.5,
                Math.max(.5, distanceKm / 150)
            );

            const target = geoToField(
                to.latitude,
                to.longitude
            );

            element.style.transition =
                `left ${durationSec}s ease, top ${durationSec}s ease`;

            element.style.left = target.left + "%";
            element.style.top = target.top + "%";

            let settled = false;

            const onDone = () => {

                if (settled) return;
                settled = true;

                element.removeEventListener("transitionend", onDone);

                index++;
                nextSegment();
            };

            element.addEventListener("transitionend", onDone);

            /*
             * Filet de securite : si transitionend ne se declenche pas
             * (ex. segment de distance quasi nulle), on avance quand meme.
             */

            setTimeout(onDone, durationSec * 1000 + 150);
        }

        nextSegment();
    });
}

/*
 * Fait partir le Coordinator de son QG (Antananarivo) et le deplace,
 * segment par segment, le long du vrai trajet jusqu'au camion en panne.
 */

async function animateCoordinatorToTruck(truck) {

    const coordinator =
        document.getElementById("coordinator");

    if (!coordinator ||
        truck.latitude == null ||
        truck.longitude == null) {
        return;
    }

    try {

        const route = await fetchRoute(
            COORDINATOR_BASE.latitude,
            COORDINATOR_BASE.longitude,
            truck.latitude,
            truck.longitude
        );

        renderRoute(route.waypoints);

        await animateElementAlongRoute(
            coordinator,
            route.waypoints
        );

    } catch (error) {

        console.error(
            "Impossible de déplacer le Coordinator :",
            error
        );
    }
}

/*
 * ================= DEAMBULATION AMBIANTE =================
 *
 * En dehors de toute panne, les camions DISPONIBLES ne doivent pas
 * rester figes sur le terrain : ils partent regulierement vers un
 * entrepot au hasard, en suivant un vrai trajet route (reseau +
 * Dijkstra), exactement comme le camion de remplacement pendant une
 * panne. Effet recherche : un terrain vivant (type colonie de
 * fourmis), pas un plan statique avec juste des etiquettes "Disponible".
 */

const AMBIENT_WANDER_INTERVAL_MS = 4000;
const AMBIENT_WANDER_PROBABILITY = 0.55;

function startAmbientWander() {
    console.log("[Déambulation] démarrée, tick toutes les", AMBIENT_WANDER_INTERVAL_MS, "ms");
    setInterval(ambientWanderTick, AMBIENT_WANDER_INTERVAL_MS);
}

async function ambientWanderTick() {

    if (!state.idmCompiled) {
        return;
    }

    if (Math.random() > AMBIENT_WANDER_PROBABILITY) {
        return;
    }

    const idleTrucks = state.trucks.filter(truck =>
        truck.status === "AVAILABLE" &&
        !state.animatingTruckIds.has(truck.id) &&
        truck.latitude != null &&
        truck.longitude != null
    );

    console.log(
        "[Déambulation] tick — camions dispo:", idleTrucks.length,
        "/ total:", state.trucks.length,
        "/ entrepôts:", state.warehouses.length
    );

    if (idleTrucks.length === 0 || state.warehouses.length === 0) {
        return;
    }

    const truck =
        idleTrucks[Math.floor(Math.random() * idleTrucks.length)];

    const candidates = state.warehouses.filter(warehouse =>
        haversineKm(
            truck.latitude, truck.longitude,
            warehouse.latitude, warehouse.longitude
        ) > 5
    );

    if (candidates.length === 0) {
        return;
    }

    const destination =
        candidates[Math.floor(Math.random() * candidates.length)];

    state.animatingTruckIds.add(truck.id);

    setSmaMessage(`${truck.code} → ${destination.name}...`);

    try {

        const route = await fetchRoute(
            truck.latitude, truck.longitude,
            destination.latitude, destination.longitude
        );

        const element = document.querySelector(
            `.real-truck[data-truck-id="${truck.id}"]`
        );

        await animateElementAlongRoute(element, route.waypoints);

        truck.latitude = destination.latitude;
        truck.longitude = destination.longitude;

        await persistTruckPosition(
            truck.id,
            destination.latitude,
            destination.longitude
        );

        setSmaMessage(`${truck.code} arrivé à ${destination.name}`);

    } catch (error) {

        console.error(
            `Déambulation impossible pour ${truck.code} :`,
            error
        );

        setSmaMessage(
            `✕ Déambulation impossible (${error.message || "voir console"})`
        );

    } finally {

        state.animatingTruckIds.delete(truck.id);
        renderTrucks();
    }
}

async function persistTruckPosition(truckId, latitude, longitude) {

    const params = new URLSearchParams({
        latitude,
        longitude
    });

    return apiFetch(
        `/trucks/${truckId}/position?${params.toString()}`,
        { method: "PATCH" }
    );
}

/* ================= SMA : CHARGEMENT ================= */

async function loadData() {

    try {

        const [trucks, warehouses] =
            await Promise.all([
                apiFetch("/trucks"),
                apiFetch("/warehouses")
            ]);

        state.trucks = trucks;
        state.warehouses = warehouses;

        renderWarehouses();
        renderTrucks();

    } catch (error) {

        console.error(
            "Impossible de charger les données SMA :",
            error
        );

        setSmaMessage(
            "✕ Impossible de charger les données du terrain."
        );
    }
}

/* ================= CAMIONS ================= */

function renderTrucks() {

    if (!els.field) return;

    const seenIds = new Set();

    state.trucks.forEach(truck => {

        seenIds.add(String(truck.id));

        const isAnimating =
            state.animatingTruckIds.has(truck.id);

        let element = els.field.querySelector(
            `.real-truck[data-truck-id="${truck.id}"]`
        );

        /*
         * Camion en cours de deplacement anime (panne, remplacement,
         * deambulation ambiante) : on ne touche pas a sa position ni
         * a sa transition CSS en cours, on met juste a jour son
         * apparence (statut/selection).
         */

        if (isAnimating && element) {

            element.classList.toggle(
                "broken",
                truck.status === "BROKEN"
            );

            element.classList.toggle(
                "selected",
                !!(state.selectedTruck && state.selectedTruck.id === truck.id)
            );

            const small = element.querySelector("small");
            if (small) small.textContent = truck.status || "";

            return;
        }

        if (element) {
            element.remove();
        }

        element =
            document.createElement("div");

        element.className =
            "agent truck real-truck";

        element.dataset.truckId =
            truck.id;

        /*
         * Position réelle venant de PostgreSQL.
         */

        const position =
            geoToField(
                truck.latitude,
                truck.longitude
            );

        element.style.left =
            position.left + "%";

        element.style.top =
            position.top + "%";

        /*
         * Apparence selon le statut.
         */

        if (truck.status === "BROKEN") {
            element.classList.add("broken");
        }

        if (
            state.selectedTruck &&
            state.selectedTruck.id === truck.id
        ) {
            element.classList.add("selected");
        }

        element.innerHTML = `
            <strong>
                ${escapeHtml(truck.code)}
            </strong>

            <small>
                ${escapeHtml(truck.status || "")}
            </small>
        `;

        element.addEventListener(
            "click",
            () => selectTruck(truck)
        );

        els.field.appendChild(element);
    });

    /*
     * Nettoyage des camions supprimes cote serveur entre deux
     * chargements.
     */

    els.field
        .querySelectorAll(".real-truck")
        .forEach(el => {
            if (!seenIds.has(el.dataset.truckId)) {
                el.remove();
            }
        });
}

/* ================= ENTREPOTS ================= */

function renderWarehouses() {

    if (!els.field) return;

    els.field
        .querySelectorAll(".warehouse")
        .forEach(element => element.remove());

    state.warehouses.forEach(warehouse => {

        const element =
            document.createElement("div");

        element.className =
            "warehouse";

        /*
         * Position réelle venant de PostgreSQL.
         */

        const position =
            geoToField(
                warehouse.latitude,
                warehouse.longitude
            );

        element.style.left =
            position.left + "%";

        element.style.top =
            position.top + "%";

        element.innerHTML = `
            <strong>
                ${escapeHtml(warehouse.name)}
            </strong>

            <small>
                ${escapeHtml(
                    warehouse.location || ""
                )}
            </small>
        `;

        els.field.appendChild(element);
    });
}

/* ================= SELECTION CAMION ================= */

function selectTruck(truck) {

    state.selectedTruck = truck;

    renderTrucks();

    setSmaMessage(
        `${truck.code} sélectionné`
    );

    /*
     * Calcule et affiche la vraie route (reseau routier + Dijkstra)
     * entre l'entrepot le plus proche et ce camion.
     */

    showRouteToTruck(truck);
}

function setSmaMessage(text) {

    if (els.smaMessage) {
        els.smaMessage.textContent = text;
    }
}

/* ================= ORCHESTRATEUR AUTONOME ================= */

const ORCHESTRATOR_POLL_MS = 4000;

/*
 * Au demarrage : memorise le dernier id d'incident deja existant (pour ne
 * pas rejouer l'historique), recupere le statut de l'orchestrateur, puis
 * lance le polling regulier qui detecte les pannes declenchees tout seul
 * cote serveur (AutoIncidentOrchestrator) et rejoue leur animation.
 */
async function initOrchestrator() {

    try {
        const latest = await apiFetch("/incidents/events/latest-id");
        state.lastIncidentId = latest.latestId || 0;
    } catch (error) {
        console.error("Impossible de récupérer le dernier incident connu :", error);
        state.lastIncidentId = 0;
    }

    await refreshOrchestratorStatus();

    setInterval(pollAutoIncidents, ORCHESTRATOR_POLL_MS);
    setInterval(refreshOrchestratorStatus, ORCHESTRATOR_POLL_MS);
}

async function refreshOrchestratorStatus() {

    try {
        const status = await apiFetch("/orchestrator/status");
        renderOrchestratorStatus(status);
    } catch (error) {
        console.error("Impossible de récupérer le statut de l'orchestrateur :", error);
    }
}

function renderOrchestratorStatus(status) {

    if (els.orchestratorToggleBtn) {

        els.orchestratorToggleBtn.classList.toggle("on", status.enabled);
        els.orchestratorToggleBtn.classList.toggle("off", !status.enabled);

        els.orchestratorToggleBtn.textContent =
            status.enabled ? "Orchestrateur : ON" : "Orchestrateur : OFF";
    }

    /*
     * L'orchestrateur declenche deja des pannes tout seul : quand il
     * est ON, le bouton manuel passe en second plan (toujours
     * cliquable, pour forcer un test) plutot que de rivaliser
     * visuellement avec l'action automatique.
     */

    if (els.breakdownBtn) {

        els.breakdownBtn.classList.toggle("subtle", status.enabled);

        els.breakdownBtn.title =
            status.enabled
                ? "L'orchestrateur déclenche déjà des pannes automatiquement — ce bouton force un test manuel."
                : "Simule une panne sur le camion sélectionné.";
    }

    if (els.orchestratorStatusEl) {

        const last = status.lastAction || "Aucune panne déclenchée.";

        els.orchestratorStatusEl.textContent =
            status.enabled ? last : `Désactivé — ${last}`;
    }
}

async function toggleOrchestrator() {

    try {
        const status = await apiFetch("/orchestrator/toggle", { method: "POST" });
        renderOrchestratorStatus(status);
    } catch (error) {
        console.error("Impossible de basculer l'orchestrateur :", error);
    }
}

/*
 * Interroge les incidents crees depuis le dernier connu et rejoue, pour
 * chacun, exactement la meme sequence visuelle qu'une panne declenchee
 * manuellement (Coordinator qui se deplace, log ACL, camion de
 * remplacement) - sauf que ni le declenchement ni le clic ne viennent de
 * l'utilisateur : c'est le SMA qui agit seul.
 */
async function pollAutoIncidents() {

    try {

        const events = await apiFetch(
            `/incidents/events/after/${state.lastIncidentId}`
        );

        for (const event of events) {

            state.lastIncidentId = Math.max(state.lastIncidentId, event.id);

            /*
             * Suit l'id le plus recent meme verrouille, pour ne pas
             * rejouer un historique entier en rafale des que le
             * modele est compile.
             */
            if (!state.idmCompiled) {
                continue;
            }

            await playAutoIncident(event);
        }

    } catch (error) {
        console.error("Erreur de polling des incidents automatiques :", error);
    }
}

async function playAutoIncident(event) {

    await loadData();

    const failedTruck =
        state.trucks.find(t => t.id === event.failedTruckId) ||
        state.trucks.find(t => t.code === event.failedTruckCode);

    if (!failedTruck) {
        return;
    }

    setSmaMessage(
        `Panne automatique détectée : ${failedTruck.code}...`
    );

    els.negotiationLog.innerHTML = "";

    await animateCoordinatorToTruck(failedTruck);

    failedTruck.status = "BROKEN";
    renderTrucks();

    (event.negotiationLog || []).forEach(line => {

        const element = document.createElement("div");
        element.className = "log-line";
        element.textContent = line;
        els.negotiationLog.appendChild(element);
    });

    if (!event.resolved || !event.replacementTruckCode) {

        setSmaMessage(
            `✕ ${event.message || "Négociation automatique échouée."}`
        );

        return;
    }

    setSmaMessage(
        `✓ ${event.replacementTruckCode} remplace ${failedTruck.code} (auto)`
    );

    const replacement =
        state.trucks.find(t => t.id === event.replacementTruckId) ||
        state.trucks.find(t => t.code === event.replacementTruckCode);

    if (!replacement) {
        return;
    }

    try {

        const route = await fetchRoute(
            replacement.latitude,
            replacement.longitude,
            failedTruck.latitude,
            failedTruck.longitude
        );

        renderRoute(route.waypoints);

        const routeLine = document.createElement("div");

        routeLine.className = "log-line";

        routeLine.textContent =
            `Route ${replacement.code} → ${failedTruck.code} : ` +
            `${route.distanceKm} km via ` +
            `${(route.roadsUsed || []).join(" → ") || "trajet direct"}`;

        els.negotiationLog.appendChild(routeLine);

        const replacementElement = document.querySelector(
            `.real-truck[data-truck-id="${replacement.id}"]`
        );

        state.animatingTruckIds.add(replacement.id);

        await animateElementAlongRoute(replacementElement, route.waypoints);

        state.animatingTruckIds.delete(replacement.id);

        replacement.latitude = failedTruck.latitude;
        replacement.longitude = failedTruck.longitude;
        replacement.status = "BUSY";

        renderTrucks();

        await persistTruckPosition(
            replacement.id,
            failedTruck.latitude,
            failedTruck.longitude
        );

    } catch (error) {
        console.error(
            "Impossible de déplacer le camion de remplacement (auto) :",
            error
        );
    }
}

/* ================= SMA : PANNE ================= */

async function simulateBreakdown() {

    if (!state.idmCompiled) {

        setSmaMessage(
            "Compile d'abord ton modèle IDM avant de lancer une simulation."
        );

        return;
    }

    if (!state.selectedTruck) {

        setSmaMessage(
            "Sélectionnez un camion avant de simuler une panne."
        );

        return;
    }

    await withLoading(
        els.breakdownBtn,
        async () => {

            const truck =
                state.selectedTruck;

            setSmaMessage(
                `Panne de ${truck.code}...`
            );

            els.negotiationLog.innerHTML = "";

            /*
             * Le Coordinator part de son QG (Antananarivo) et se
             * deplace, le long du vrai trajet, jusqu'au camion en panne.
             */

            await animateCoordinatorToTruck(truck);

            try {

                const result =
                    await apiFetch(
                        `/simulation/breakdown/${truck.id}`,
                        {
                            method: "POST"
                        }
                    );

                /*
                 * Mise à jour du camion en panne.
                 */

                truck.status = "BROKEN";

                renderTrucks();

                /*
                 * Affichage de la vraie trace ACL.
                 */

                (result.negotiationLog || [])
                    .forEach(line => {

                        const element =
                            document.createElement("div");

                        element.className =
                            "log-line";

                        element.textContent =
                            line;

                        els.negotiationLog
                            .appendChild(element);
                    });

                /*
                 * Résultat réel du backend.
                 */

                setSmaMessage(
                    `✓ ${result.replacementTruck} remplace ${result.failedTruck}`
                );

                /*
                 * Empeche le polling automatique de rejouer cette meme
                 * panne (deja animee ici) au prochain tick.
                 */
                try {
                    const latest = await apiFetch("/incidents/events/latest-id");
                    state.lastIncidentId = Math.max(state.lastIncidentId || 0, latest.latestId || 0);
                } catch (syncError) {
                    console.error("Impossible de synchroniser le dernier incident :", syncError);
                }

                /*
                 * Camion de remplacement : calcul du vrai trajet, puis
                 * deplacement anime segment par segment jusqu'a la
                 * position du camion en panne (il prend le relais sur
                 * place).
                 */

                const replacement = state.trucks.find(
                    candidate => candidate.code === result.replacementTruck
                );

                if (replacement) {

                    try {

                        const route = await fetchRoute(
                            replacement.latitude,
                            replacement.longitude,
                            truck.latitude,
                            truck.longitude
                        );

                        renderRoute(route.waypoints);

                        const routeLine = document.createElement("div");

                        routeLine.className = "log-line";

                        routeLine.textContent =
                            `Route ${replacement.code} → ${truck.code} : ` +
                            `${route.distanceKm} km via ` +
                            `${(route.roadsUsed || []).join(" → ") || "trajet direct"}`;

                        els.negotiationLog.appendChild(routeLine);

                        /*
                         * L'element DOM du camion vient d'etre recree
                         * par renderTrucks() ci-dessus : on le recupere
                         * maintenant, juste avant de l'animer.
                         */

                        const replacementElement =
                            document.querySelector(
                                `.real-truck[data-truck-id="${replacement.id}"]`
                            );

                        state.animatingTruckIds.add(replacement.id);

                        await animateElementAlongRoute(
                            replacementElement,
                            route.waypoints
                        );

                        state.animatingTruckIds.delete(replacement.id);

                        /*
                         * Arrivee : mise a jour de l'etat local puis
                         * persistance reelle en base (Postgres), pour que
                         * le terrain et la base restent coherents.
                         */

                        replacement.latitude = truck.latitude;
                        replacement.longitude = truck.longitude;
                        replacement.status = "BUSY";

                        renderTrucks();

                        await persistTruckPosition(
                            replacement.id,
                            truck.latitude,
                            truck.longitude
                        );

                    } catch (error) {

                        console.error(
                            "Impossible de déplacer le camion de remplacement :",
                            error
                        );
                    }
                }

            } catch (error) {

                console.error(
                    "Échec de la simulation :",
                    error
                );

                setSmaMessage(
                    `✕ ${error.message}`
                );
            }
        }
    );
}

/* ================= AJOUT CAMION ================= */

function openAddTruckModal() {

    openModal({

        title: "Ajouter un camion",

        fields: [

            {
                name: "code",
                label: "Code camion",
                type: "text",
                required: true
            },

            {
                name: "driver",
                label: "Conducteur",
                type: "text",
                required: true
            },

            {
                name: "capacity",
                label: "Capacité (kg)",
                type: "number",
                value: 1000,
                required: true
            },

            {
                name: "latitude",
                label: "Latitude",
                type: "number",
                value: -18.8792,
                required: true
            },

            {
                name: "longitude",
                label: "Longitude",
                type: "number",
                value: 47.5079,
                required: true
            },

            {
                name: "refrigerated",
                label: "Camion réfrigéré",
                type: "checkbox"
            }
        ],

        onSubmit: async values => {

            await apiFetch(
                "/trucks",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body: JSON.stringify({

                        code: values.code,

                        driver: values.driver,

                        capacity:
                            Number(values.capacity),

                        latitude:
                            Number(values.latitude),

                        longitude:
                            Number(values.longitude),

                        status: "AVAILABLE",

                        refrigerated:
                            Boolean(values.refrigerated)
                    })
                }
            );

            await loadData();
        }
    });
}

/* ================= AJOUT ENTREPOT ================= */

function openAddWarehouseModal() {

    openModal({

        title: "Ajouter un entrepôt",

        fields: [

            {
                name: "name",
                label: "Nom de l'entrepôt",
                type: "text",
                required: true
            },

            {
                name: "location",
                label: "Localisation",
                type: "text",
                required: true
            },

            {
                name: "latitude",
                label: "Latitude",
                type: "number",
                required: true
            },

            {
                name: "longitude",
                label: "Longitude",
                type: "number",
                required: true
            }
        ],

        onSubmit: async values => {

            await apiFetch(
                "/warehouses",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body: JSON.stringify({

                        name: values.name,

                        location:
                            values.location,

                        latitude:
                            Number(values.latitude),

                        longitude:
                            Number(values.longitude)
                    })
                }
            );

            await loadData();
        }
    });
}

/* ================= MODALE ================= */

let activeModalSubmit = null;

function openModal({
    title,
    fields,
    onSubmit
}) {

    els.modalTitle.textContent =
        title;

    els.modalFields.innerHTML = "";

    hideModalError();

    fields.forEach(field => {

        const wrapper =
            document.createElement("div");

        wrapper.className =
            field.type === "checkbox"
                ? "modal-field checkbox"
                : "modal-field";

        const label =
            document.createElement("label");

        label.textContent =
            field.label;

        label.htmlFor =
            `field-${field.name}`;

        const input =
            document.createElement("input");

        input.type =
            field.type;

        input.id =
            `field-${field.name}`;

        input.name =
            field.name;

        if (field.required) {
            input.required = true;
        }

        if (field.value !== undefined) {

            if (field.type === "checkbox") {

                input.checked =
                    Boolean(field.value);

            } else {

                input.value =
                    field.value;
            }
        }

        if (field.type === "checkbox") {

            wrapper.append(
                input,
                label
            );

        } else {

            wrapper.append(
                label,
                input
            );
        }

        els.modalFields.appendChild(
            wrapper
        );
    });

    activeModalSubmit =
        onSubmit;

    els.modal.classList.remove(
        "hidden"
    );

    els.modalFields
        .querySelector("input")
        ?.focus();
}

function closeModal() {

    els.modal.classList.add(
        "hidden"
    );

    activeModalSubmit = null;

    els.modalForm.reset();

    hideModalError();
}

function showModalError(message) {

    els.modalError.textContent =
        message;

    els.modalError.classList.remove(
        "hidden"
    );
}

function hideModalError() {

    els.modalError.classList.add(
        "hidden"
    );

    els.modalError.textContent =
        "";
}

document.addEventListener(
    "DOMContentLoaded",
    () => {

        const modalForm =
            document.getElementById(
                "modalForm"
            );

        if (!modalForm) return;

        modalForm.addEventListener(
            "submit",
            async event => {

                event.preventDefault();

                if (!activeModalSubmit)
                    return;

                const formData =
                    new FormData(
                        modalForm
                    );

                const values = {};

                modalForm
                    .querySelectorAll("input")
                    .forEach(input => {

                        values[input.name] =
                            input.type === "checkbox"
                                ? input.checked
                                : formData.get(
                                    input.name
                                );
                    });

                const submitBtn =
                    modalForm.querySelector(
                        'button[type="submit"]'
                    );

                await withLoading(
                    submitBtn,
                    async () => {

                        try {

                            await activeModalSubmit(
                                values
                            );

                            closeModal();

                        } catch (error) {

                            console.error(
                                "Échec formulaire :",
                                error
                            );

                            showModalError(
                                error.message ||
                                "Une erreur est survenue."
                            );
                        }
                    }
                );
            }
        );
    }
);

/* ================= IDM ================= */

async function loadIdmSample() {

    await withLoading(
        els.loadSampleBtn,
        async () => {

            try {

                els.idmSource.value =
                    await apiFetch(
                        "/idm/sample"
                    );

            } catch (error) {

                console.error(
                    "Impossible de charger IDM :",
                    error
                );

                els.idmInfo.textContent =
                    "✕ Impossible de charger l'exemple.";
            }
        }
    );
}

/*
 * Fait progresser visuellement le pipeline IDM
 * (Lexer → Parser → Validation → Model → Java)
 * pendant la compilation : chaque etape passe par
 * "active" (balayage lumineux) puis "done" (vert),
 * independamment du temps reel de la requete /idm/compile.
 */

function animatePipeline() {

    const nodes = [
        document.getElementById("idm-lexer"),
        document.getElementById("idm-parser"),
        document.getElementById("idm-validator"),
        document.getElementById("idm-model"),
        document.getElementById("idm-java")
    ];

    const arrows =
        document.querySelectorAll(".pipeline .arrow");

    // Reset
    nodes.forEach(node => {
        node?.classList.remove("active", "done");
    });

    arrows.forEach(arrow => {
        arrow.classList.remove("done");
    });

    return new Promise(resolve => {

        // Progression
        nodes.forEach((node, index) => {

            setTimeout(() => {

                // Etape precedente terminee
                if (index > 0) {

                    nodes[index - 1]?.classList.remove("active");
                    nodes[index - 1]?.classList.add("done");

                    if (arrows[index - 1]) {
                        arrows[index - 1].classList.add("done");
                    }
                }

                // Etape actuelle
                node?.classList.add("active");

            }, index * 700);

        });

        // Java termine
        setTimeout(() => {

            const last = nodes[nodes.length - 1];

            last?.classList.remove("active");
            last?.classList.add("done");

            resolve();

        }, nodes.length * 700);
    });
}

async function compileIdm() {

    const source =
        els.idmSource.value.trim();

    if (!source) {

        els.idmInfo.textContent =
            "Le modèle FlexNet est vide.";

        return;
    }

    await withLoading(
        els.compileBtn,
        async () => {

            /*
             * Lance l'animation du pipeline (Lexer → ... → Java) en
             * parallele de l'appel reseau : elle dure toujours ~3.5s,
             * independamment du temps de reponse du backend.
             */

            const pipelineAnimation =
                animatePipeline();

            try {

                const result =
                    await apiFetch(
                        "/idm/compile",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/json"
                            },

                            body: JSON.stringify({
                                source
                            })
                        }
                    );

                await pipelineAnimation;

                els.idmInfo.innerHTML = `
                    <strong>
                        ✓ ${escapeHtml(
                            result.networkName
                        )}
                    </strong>

                    <br>

                    Entrepôts :
                    ${result.warehouseCount}

                    · Camions :
                    ${result.truckCount}

                    · Commandes :
                    ${result.orderCount}

                    <br>

                    Classe :
                    ${escapeHtml(
                        result.generatedClassName
                    )}
                `;

                els.generatedJava.textContent =
                    result.generatedJavaSource;

                /*
                 * Le code est genere : les simulations (panne,
                 * orchestrateur, deambulation ambiante) deviennent
                 * disponibles.
                 */
                setSimulationLocked(false);

                setSmaMessage(
                    "Modèle compilé — simulations débloquées."
                );

            } catch (error) {

                console.error(
                    "Échec compilation IDM :",
                    error
                );

                els.idmInfo.textContent =
                    `✕ ${error.message}`;
            }
        }
    );
}

/* ================= OUTILS ================= */

function escapeHtml(text) {

    return String(text ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}