SIMULATION FleetManagement_v1 {
    
    CONFIGURATION {
        speed: 1.0x,
        tickMs: 100,
        autoResolve: true,
        breakdownChance: 0.03%,
        stockTransferAmount: 30,
    }
    
    WAREHOUSES {
        WH-001 {
            name: "Entrepôt A",
            position: (12%, 20%),
            stock: 100,
            maxStock: 150,
        }
        WH-002 {
            name: "Entrepôt B",
            position: (80%, 18%),
            stock: 80,
            maxStock: 120,
        }
        WH-003 {
            name: "Entrepôt C",
            position: (20%, 72%),
            stock: 120,
            maxStock: 180,
        }
        WH-004 {
            name: "Entrepôt D",
            position: (78%, 74%),
            stock: 90,
            maxStock: 140,
        }
    }
    
    TRUCKS {
        T-001 {
            code: "T-01",
            speed: 10.5,
            maxLoad: 50,
            startWarehouse: WH-001,
            initialStatus: IDLE,
        }
        T-002 {
            code: "T-02",
            speed: 12.0,
            maxLoad: 50,
            startWarehouse: WH-002,
            initialStatus: IDLE,
        }
        T-003 {
            code: "T-03",
            speed: 9.5,
            maxLoad: 50,
            startWarehouse: WH-003,
            initialStatus: IDLE,
        }
        T-004 {
            code: "T-04",
            speed: 11.0,
            maxLoad: 50,
            startWarehouse: WH-004,
            initialStatus: IDLE,
        }
    }
    
    AGENTS {
        Agent-Dispatcher {
            type: Dispatcher,
            capabilities: [assign_truck, create_replacement, remove_truck, add_truck],
            decisionRules: [
                "IF incident.type == TRUCK_BREAKDOWN THEN find_nearest_available_truck()",
                "IF truck.status == IDLE THEN assign_random_destination()",
            ],
        }
        Agent-Logistics {
            type: Logistics,
            capabilities: [coordinate_movement, manage_loading, manage_unloading],
            decisionRules: [
                "IF truck.currentLoad == 0 THEN find_warehouse_with_stock()",
                "IF truck.currentLoad > 0 THEN find_warehouse_for_unload()",
            ],
        }
        Agent-Repair {
            type: Repair,
            capabilities: [dispatch_repair, repair_on_site],
            decisionRules: [
                "IF incident.type == TRUCK_BREAKDOWN AND NOT has_replacement THEN repair_on_site()",
            ],
        }
        Agent-Router {
            type: Router,
            capabilities: [reroute, calculate_path, avoid_incident],
            decisionRules: [
                "IF incident.type == ROAD_BLOCKED THEN reroute_affected_trucks()",
            ],
        }
        Agent-StockManager {
            type: StockManager,
            capabilities: [monitor_stock, transfer_stock, alert_shortage],
            decisionRules: [
                "IF warehouse.stock < 50 THEN trigger_stock_transfer()",
                "IF incident.type == STOCK_SHORTAGE THEN redistribute_stock()",
            ],
        }
        Agent-Supervisor {
            type: Supervisor,
            capabilities: [monitor_all, override_decisions, generate_reports],
            decisionRules: [
                "IF incidents.count > 5 THEN alert_critical()",
                "IF total_stock < 100 THEN alert_low_stock()",
            ],
        }
    }
    
    INCIDENTS {
        types: {
            TRUCK_BREAKDOWN {
                label: "Panne de camion",
                icon: AlertTriangle,
                color: red,
                autoResolve: true,
                resolveTime: 2500ms,
                effects: [
                    set_truck_status(BROKEN),
                    block_movement,
                ],
            }
            ROAD_BLOCKED {
                label: "Route bloquée",
                icon: Ban,
                color: amber,
                autoResolve: true,
                resolveTime: 2000ms,
                effects: [
                    block_route,
                    trigger_reroute,
                ],
            }
            STOCK_SHORTAGE {
                label: "Pénurie de stock",
                icon: Package,
                color: orange,
                autoResolve: true,
                resolveTime: 3000ms,
                effects: [
                    prevent_loading,
                    trigger_transfer,
                ],
            }
        }
        generation: {
            TRUCK_BREAKDOWN: 45%,
            ROAD_BLOCKED: 30%,
            STOCK_SHORTAGE: 25%,
            interval: 2500ms,
        }
    }
    
    RULES {
        TruckBreakdownHandling {
            priority: 1,
            trigger: incident.type == TRUCK_BREAKDOWN,
            conditions: [
                incident.status == OPEN,
                truck.status == BROKEN,
            ],
            actions: [
                create_incident(incident),
                find_available_trucks(),
                sort_by_distance(incident.location),
                assign_replacement(nearest_truck, incident),
                set_status(incident.truckId, RETURNING),
                log("Panne: " + incident.truckId + " - Remplacement: " + nearest_truck.code, error),
            ],
            agent: Agent-Dispatcher,
        }
        TruckMovement {
            priority: 2,
            trigger: truck.status == IDLE,
            conditions: [
                truck.currentLoad == 0,
                exists(warehouse WHERE warehouse.stock > 10),
            ],
            actions: [
                find_warehouses_with_stock(),
                sort_by_distance(truck.position),
                assign_destination(nearest_warehouse),
                set_status(truck.id, MOVING),
                log(truck.code + " se dirige vers " + warehouse.name, info),
            ],
            agent: Agent-Logistics,
        }
        StockManagement {
            priority: 3,
            trigger: truck.arrival_at_warehouse,
            conditions: [
                truck.currentLoad == 0,
                warehouse.stock > 10,
            ],
            actions: [
                load_amount = min(30, warehouse.stock, truck.maxLoad),
                decrease_stock(warehouse.id, load_amount),
                set_truck_load(truck.id, load_amount),
                set_status(truck.id, LOADING),
                log(truck.code + " charge " + load_amount + "u à " + warehouse.name, info),
            ],
            agent: Agent-Logistics,
        }
        StockUnloading {
            priority: 4,
            trigger: truck.arrival_at_warehouse,
            conditions: [
                truck.currentLoad > 0,
            ],
            actions: [
                unload_amount = truck.currentLoad,
                increase_stock(warehouse.id, unload_amount),
                set_truck_load(truck.id, 0),
                set_status(truck.id, UNLOADING),
                log(truck.code + " décharge " + unload_amount + "u à " + warehouse.name, success),
            ],
            agent: Agent-Logistics,
        }
        StockShortageResponse {
            priority: 5,
            trigger: incident.type == STOCK_SHORTAGE,
            conditions: [
                incident.currentStock < 50,
                exists(warehouse WHERE warehouse.stock > 20),
            ],
            actions: [
                find_warehouse_with_max_stock(),
                transfer_stock(donor_warehouse, incident.warehouseId, 30),
                decrease_stock(donor_warehouse.id, 30),
                increase_stock(incident.warehouseId, 30),
                log("Transfert de stock: " + donor_warehouse.name + " -> " + incident.warehouseId, success),
            ],
            agent: Agent-StockManager,
        }
        RoadBlockHandling {
            priority: 6,
            trigger: incident.type == ROAD_BLOCKED,
            conditions: [
                incident.status == OPEN,
                exists(truck WHERE truck.destination == incident.affected_route),
            ],
            actions: [
                find_affected_trucks(),
                calculate_alternative_routes(),
                assign_new_destination(affected_truck, alternative_warehouse),
                set_status(affected_truck.id, REROUTING),
                log("Reroutage: " + affected_truck.code + " vers " + alternative_warehouse.name, warning),
            ],
            agent: Agent-Router,
        }
        BreakdownChance {
            priority: 7,
            trigger: random_event,
            conditions: [
                truck.status == MOVING,
                truck.isReplacement == false,
                random() < 0.03,
            ],
            actions: [
                set_status(truck.id, BROKEN),
                create_incident(TRUCK_BREAKDOWN, truck.location),
                log("Panne automatique: " + truck.code, error),
            ],
            agent: Agent-Supervisor,
        }
    }
}