package com.flexchain.idm;

import com.flexchain.idm.codegen.JavaDataLoaderGenerator;
import com.flexchain.idm.dsl.Lexer;
import com.flexchain.idm.dsl.Parser;
import com.flexchain.idm.dsl.SemanticValidator;
import com.flexchain.idm.dsl.Token;
import com.flexchain.idm.dsl.ast.NetworkModel;
import jakarta.validation.Valid;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;


@RestController
@RequestMapping("/idm")
@CrossOrigin("*")
public class IdmController {

    private final JavaDataLoaderGenerator generator = new JavaDataLoaderGenerator();
    private final NetworkDeployService deployService;

    public IdmController(NetworkDeployService deployService) {
        this.deployService = deployService;
    }

    @PostMapping("/compile")
    public CompileResponse compile(@Valid @RequestBody CompileRequest request) {
        NetworkModel model = compileToModel(request);
        String javaSource = generator.generate(model);

        return CompileResponse.builder()
                .networkName(model.name)
                .warehouseCount(model.warehouses.size())
                .truckCount(model.trucks.size())
                .orderCount(model.orders.size())
                .generatedClassName(generator.suggestedFileName(model).replace(".java", ""))
                .generatedJavaSource(javaSource)
                .build();
    }

   
    @PostMapping("/deploy")
    public DeployResponse deploy(@Valid @RequestBody CompileRequest request) {
        NetworkModel model = compileToModel(request);
        NetworkDeployService.DeployResult result = deployService.deploy(model);

        return DeployResponse.builder()
                .networkName(result.networkName())
                .warehousesCreated(result.warehousesCreated())
                .trucksCreated(result.trucksCreated())
                .ordersCreated(result.ordersCreated())
                .message("Réseau '" + result.networkName() + "' déployé : " +
                        result.warehousesCreated() + " entrepôt(s), " +
                        result.trucksCreated() + " camion(s), " +
                        result.ordersCreated() + " commande(s) créés dans le SMA.")
                .build();
    }

    private NetworkModel compileToModel(CompileRequest request) {
        List<Token> tokens = new Lexer(request.getSource()).tokenize();
        NetworkModel model = new Parser(tokens).parse();
        new SemanticValidator().validate(model);
        return model;
    }

    /**
     * Renvoie le modele d'exemple embarque, pratique pour peupler l'UI de demo.
     */
    @GetMapping("/sample")
    public String sample() throws IOException {
        return new String(new ClassPathResource("flexnet-samples/demo-network.flexnet")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
