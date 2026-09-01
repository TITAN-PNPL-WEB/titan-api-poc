package poc.titan.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import poc.titan.api.dto.ValidationRequest;
import poc.titan.api.dto.ValidationResponse;
import poc.titan.api.service.ValidationService;

/**
 * Endpoint for validating a .vrb file before it gets analyzed.
 */
@RestController
@RequestMapping("/pnpl")
public class ValidationController {

    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    /**
     * Validates the .vrb at the given path and returns any issues found.
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validate(@RequestBody ValidationRequest req) {
        if (req.getVrbPath() == null || req.getVrbPath().isBlank()) {
            throw new IllegalArgumentException("vrbPath is required");
        }
        ValidationResponse response = validationService.validate(req.getVrbPath());
        return ResponseEntity.ok(response);
    }
}