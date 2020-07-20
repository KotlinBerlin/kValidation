package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.internal.BasicAndValidationBuilder

interface Validation<T> {

    companion object {
        operator fun <T> invoke(init: AndValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = BasicAndValidationBuilder<T>()
            return builder.apply(init).build()
        }
    }

    fun validate(aValue: T): ValidationResult<T>
    operator fun invoke(value: T) = validate(value)
}
