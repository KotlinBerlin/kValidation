package de.kotlinBerlin.kValidation

sealed class ValidationResult<in T> {
    abstract operator fun get(vararg propertyPath: Any): List<String>?
    abstract fun <R> withValue(transform: () -> R): ValidationResult<R>
    abstract val errors: ValidationErrors
}

class Invalid<T>(
    internal val internalErrors: Map<ValidationPath, List<String>>
) : ValidationResult<T>() {

    override fun get(vararg propertyPath: Any): List<String>? =
        internalErrors.entries.filter { it.key.matches(propertyPath) }.flatMap { it.value }

    override fun <R> withValue(transform: () -> R): ValidationResult<R> = Invalid(this.internalErrors)

    override val errors: ValidationErrors by lazy {
        DefaultValidationErrors((internalErrors).flatMap { (path, errors) ->
            errors.map { BasicValidationError(path, it) }
        })
    }

    override fun toString(): String = "Invalid(errors=${errors})"
}

data class Valid<T>(val value: T) : ValidationResult<T>() {
    override fun get(vararg propertyPath: Any): List<String>? = null
    override fun <R> withValue(transform: () -> R): ValidationResult<R> = Valid(transform())
    override val errors: ValidationErrors get() = NoValidationErrors
}

data class NoResult<T>(val value: T) : ValidationResult<T>() {
    override fun get(vararg propertyPath: Any): List<String>? = null
    override fun <R> withValue(transform: () -> R): ValidationResult<R> = NoResult(transform())
    override val errors: ValidationErrors get() = NoValidationErrors
}