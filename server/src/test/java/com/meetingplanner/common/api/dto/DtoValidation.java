package com.meetingplanner.common.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

// Runs Bean Validation on a request record the way @Valid does, without a Spring context.
final class DtoValidation {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private DtoValidation() {
    }

    static Set<String> violatedPaths(Object request) {
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
