package de.kotlinBerlin.kValidation.constraints

import de.kotlinBerlin.kValidation.ValidationBuilder

fun ValidationBuilder<String>.minLength(length: Int): Constraint<String> {
    require(length >= 0) { IllegalArgumentException("minLength requires the length to be >= 0") }
    return addConstraint(
        "must have at least {0} characters",
        length.toString()
    ) { tempValue, _ -> tempValue.length >= length }
}

fun ValidationBuilder<String>.maxLength(length: Int): Constraint<String> {
    require(length >= 0) { IllegalArgumentException("maxLength requires the length to be >= 0") }
    return addConstraint(
        "must have at most {0} characters",
        length.toString()
    ) { tempValue, _ -> tempValue.length <= length }
}

fun ValidationBuilder<String>.pattern(pattern: String) = pattern(pattern.toRegex())

fun ValidationBuilder<String>.pattern(pattern: Regex) = addConstraint(
    "must match the expected pattern",
    pattern.toString()
) { tempValue, _ -> tempValue.matches(pattern) }