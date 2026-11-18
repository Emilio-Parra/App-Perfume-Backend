package com.tienda.perfumes.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Lógica de la anotación @RutValido (algoritmo módulo 11).
 */
public class RutValidator implements ConstraintValidator<RutValido, String> {

    @Override
    public boolean isValid(String rut, ConstraintValidatorContext context) {
        if (rut == null || rut.isBlank()) {
            return true; // @NotBlank se encarga de exigirlo
        }
        return esValido(rut.trim());
    }

    /**
     * Comprueba el dígito verificador de un RUN sin puntos ni guion.
     * Público y estático para poder reutilizarlo y probarlo en los tests.
     */
    public static boolean esValido(String rut) {
        if (rut == null || !rut.matches("^[0-9]{6,8}[0-9kK]$")) {
            return false;
        }

        String cuerpo = rut.substring(0, rut.length() - 1);
        char dv = Character.toUpperCase(rut.charAt(rut.length() - 1));

        int suma = 0;
        int multiplicador = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += multiplicador * Character.getNumericValue(cuerpo.charAt(i));
            multiplicador = multiplicador < 7 ? multiplicador + 1 : 2;
        }

        int resto = 11 - (suma % 11);
        char esperado = resto == 11 ? '0' : resto == 10 ? 'K' : Character.forDigit(resto, 10);

        return dv == esperado;
    }
}
