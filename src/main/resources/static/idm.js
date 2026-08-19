const API = "http://localhost:8080";

const positions = {

    source: 8,
    lexer: 23,
    parser: 38,
    validator: 53,
    model: 68,
    generator: 83,
    java: 94

};

async function loadSample() {

    try {

        setMessage(
            "Chargement du modèle FlexNet..."
        );


        const response =
            await fetch(
                `${API}/idm/sample`
            );


        if (!response.ok) {

            throw new Error(
                `HTTP ${response.status}`
            );
        }


        const source =
            await response.text();


        document
            .getElementById("source")
            .value = source;


        resetTerrain();


        setNode(
            "node-source",
            "done"
        );


        setMessage(
            "Modèle FlexNet chargé"
        );


    } catch (error) {

        console.error(error);

        setMessage(
            "Erreur : impossible de charger le modèle"
        );
    }
}

async function runIdm() {

    const source =
        document
            .getElementById("source")
            .value
            .trim();


    if (!source) {

        setMessage(
            "Aucun modèle FlexNet"
        );

        return;
    }


    resetTerrain();

    await visit(
        "source",
        "Lecture du modèle FlexNet"
    );

    await visit(
        "lexer",
        "Analyse lexicale → création des tokens"
    );

    await visit(
        "parser",
        "Analyse syntaxique → construction du modèle"
    );

    try {

        setMessage(
            "Exécution du pipeline réel..."
        );


        const response =
            await fetch(
                `${API}/idm/compile`,
                {

                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify({
                            source: source
                        })

                }
            );


        if (!response.ok) {

            const text =
                await response.text();

            throw new Error(
                text || `HTTP ${response.status}`
            );
        }


        const result =
            await response.json();

        await visit(
            "validator",
            "Validation sémantique réussie"
        );

        await visit(
            "model",
            "NetworkModel construit"
        );


        showModelResult(
            result
        );

        await visit(
            "generator",
            "Génération du code Java"
        );

        await visit(
            "java",
            "Java généré avec succès"
        );


        showJava(
            result
        );


        setMessage(
            "✓ Pipeline IDM terminé"
        );

        document
            .getElementById("deployBtn")
            .disabled = false;


    } catch (error) {

        console.error(error);


        setNode(
            "node-validator",
            "error"
        );


        setMessage(
            "✕ Erreur IDM : " +
            cleanError(error.message)
        );
    }
}

async function deployIdm() {

    const source =
        document
            .getElementById("source")
            .value
            .trim();

    const deployBtn =
        document.getElementById("deployBtn");

    const deployMessage =
        document.getElementById("deployMessage");

    if (!source) {
        return;
    }

    deployBtn.disabled = true;
    deployMessage.className = "deploy-message";
    deployMessage.textContent = "Déploiement en cours...";

    try {

        const response =
            await fetch(
                `${API}/idm/deploy`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({ source: source })
                }
            );

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.message || `HTTP ${response.status}`
            );
        }

        deployMessage.className = "deploy-message success";
        deployMessage.textContent = "✓ " + result.message;

    } catch (error) {

        console.error(error);

        deployMessage.className = "deploy-message error";
        deployMessage.textContent =
            "✕ Erreur de déploiement : " + cleanError(error.message);

    } finally {
        deployBtn.disabled = false;
    }
}


async function visit(
    name,
    message
) {

    const node =
        document.getElementById(
            `node-${name}`
        );

    movePacket(
        positions[name]
    );

    setNode(
        `node-${name}`,
        "active"
    );


    setMessage(
        message
    );


    await wait(800);

    setNode(
        `node-${name}`,
        "done"
    );
}

function movePacket(
    position
) {

    const packet =
        document.getElementById(
            "packet"
        );


    packet.classList.add(
        "moving"
    );


    packet.style.left =
        position + "%";
}

function setNode(
    id,
    state
) {

    const node =
        document.getElementById(id);


    node.classList.remove(
        "active",
        "done",
        "error"
    );


    if (state) {

        node.classList.add(
            state
        );
    }
}

function showModelResult(
    result
) {

    document
        .getElementById(
            "modelStats"
        )
        .classList.remove(
            "hidden"
        );


    document
        .getElementById(
            "networkName"
        )
        .textContent =
        result.networkName || "-";


    document
        .getElementById(
            "warehouseCount"
        )
        .textContent =
        result.warehouseCount ?? 0;


    document
        .getElementById(
            "truckCount"
        )
        .textContent =
        result.truckCount ?? 0;


    document
        .getElementById(
            "orderCount"
        )
        .textContent =
        result.orderCount ?? 0;

    const resultContent =
        document.getElementById(
            "resultContent"
        );


    resultContent.innerHTML = `

        <div class="result-title">
            ✓ NetworkModel
        </div>

        <div class="result-code">

Réseau : ${escapeHtml(
        result.networkName
    )}

Entrepôts : ${
        result.warehouseCount
    }

Camions : ${
        result.truckCount
    }

Commandes : ${
        result.orderCount
    }

        </div>

    `;
}

function showJava(
    result
) {

    document
        .getElementById(
            "className"
        )
        .textContent =
        result.generatedClassName
            ? result.generatedClassName +
              ".java"
            : "Java généré";


    document
        .getElementById(
            "javaOutput"
        )
        .textContent =
        result.generatedJavaSource ||
        "// Aucun code généré";
}

function resetTerrain() {

    const nodes = [
        "source",
        "lexer",
        "parser",
        "validator",
        "model",
        "generator",
        "java"
    ];


    nodes.forEach(
        name =>
            setNode(
                `node-${name}`,
                null
            )
    );


    const packet =
        document.getElementById(
            "packet"
        );


    packet.classList.remove(
        "moving"
    );


    packet.style.left =
        positions.source + "%";


    document
        .getElementById(
            "modelStats"
        )
        .classList.add(
            "hidden"
        );


    document
        .getElementById(
            "resultContent"
        )
        .innerHTML = `

        <div class="empty-result">

            <div>◇</div>

            <p>
                Lance le pipeline pour voir
                ce que chaque étape produit.
            </p>

        </div>
    `;


    document
        .getElementById(
            "className"
        )
        .textContent =
        "En attente...";


    document
        .getElementById(
            "javaOutput"
        )
        .textContent =
        "Le code Java généré apparaîtra ici.";

    document
        .getElementById("deployBtn")
        .disabled = true;

    const deployMessage =
        document.getElementById("deployMessage");

    deployMessage.className = "deploy-message";
    deployMessage.textContent = "";
}

function setMessage(
    message
) {

    document
        .getElementById(
            "fieldMessage"
        )
        .textContent =
        message;
}

function wait(
    milliseconds
) {

    return new Promise(
        resolve =>
            setTimeout(
                resolve,
                milliseconds
            )
    );
}


function cleanError(
    error
) {

    if (!error) {

        return "Erreur inconnue";
    }


    return error
        .replace(
            /<[^>]*>/g,
            ""
        )
        .substring(
            0,
            300
        );
}


function escapeHtml(
    text
) {

    if (text === null ||
        text === undefined) {

        return "";
    }


    return String(text)
        .replace(
            /&/g,
            "&amp;"
        )
        .replace(
            /</g,
            "&lt;"
        )
        .replace(
            />/g,
            "&gt;"
        )
        .replace(
            /"/g,
            "&quot;"
        )
        .replace(
            /'/g,
            "&#039;"
        );
}

window.addEventListener(
    "load",
    () => {

        loadSample();

    }
);