package com.simnote.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Size constraint applied to the trimmed value; null, blank, or whitespace-only values are invalid. */
@Documented
@Constraint(validatedBy = TrimmedSize.Validator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TrimmedSize {

    String message() default "must not be blank and must be between {min} and {max} characters after trimming";

    int min() default 1;

    int max() default Integer.MAX_VALUE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<TrimmedSize, String> {

        private int min;
        private int max;

        @Override
        public void initialize(TrimmedSize annotation) {
            this.min = annotation.min();
            this.max = annotation.max();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return false;
            }
            int length = value.strip().length();
            return length >= min && length <= max;
        }
    }
}
