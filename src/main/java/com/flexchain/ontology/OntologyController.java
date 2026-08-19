package com.flexchain.ontology;

import com.flexchain.entity.Order;
import com.flexchain.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Expose le raisonnement ontologique FlexChain via HTTP :
 *  - POST /ontology/evaluate       : evaluation ad-hoc (demo, sans donnees persistees)
 *  - GET  /ontology/evaluate/{id}  : evaluation d'une commande existante en base
 */
@RestController
@RequestMapping("/ontology")
@RequiredArgsConstructor
@CrossOrigin("*")
public class OntologyController {

    private final OntologyReasoningService ontologyReasoningService;
    private final OrderService orderService;

    @PostMapping("/evaluate")
    public OntologyEvaluationResult evaluate(@Valid @RequestBody OntologyEvaluateRequest request) {
        String orderId = request.getOrderId() != null ? request.getOrderId() : ("adhoc-" + System.currentTimeMillis());
        return ontologyReasoningService.evaluate(orderId, Boolean.TRUE.equals(request.getFragile()),
                request.getCurrentTemperatureCelsius());
    }

    @GetMapping("/evaluate/{orderId}")
    public OntologyEvaluationResult evaluateOrder(@PathVariable Long orderId) {
        Order order = orderService.findById(orderId);
        return ontologyReasoningService.evaluate(String.valueOf(order.getId()),
                Boolean.TRUE.equals(order.getFragile()), order.getCurrentTemperatureCelsius());
    }
}
