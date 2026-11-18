package com.tienda.perfumes.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida un RUN chileno sin puntos ni guion (ej. 19011022K)
 * usando el algoritmo módulo 11, igual que el frontend.
 * Un valor nulo o vacío se considera válido: para exigirlo se usa @NotBlank.
 */
@Documented
@Constraint(validatedBy = RutValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface RutValido {

    String message() default "RUN inválido. Escríbelo sin puntos ni guion (ej. 19011022K).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
