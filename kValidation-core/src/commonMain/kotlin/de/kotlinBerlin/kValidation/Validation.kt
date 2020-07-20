package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.internal.BasicAndValidationBuilder
import de.kotlinBerlin.kValidation.internal.BasicOrValidationBuilder

interface Validation<in T> {

    companion object {
        operator fun <T> invoke(init: AndValidationBuilder<T>.() -> Unit): Validation<T> =
            and(init)

        fun <T> and(init: AndValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = BasicAndValidationBuilder<T>()
            return builder.apply(init).build()
        }

        fun <T> or(init: OrValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = BasicOrValidationBuilder<T>()
            return builder.apply(init).build()
        }
    }

    fun validate(aValue: T): ValidationResult<T>
    operator fun invoke(value: T) = validate(value)
}
