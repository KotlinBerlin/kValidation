package de.kotlinBerlin.kValidation

/** The result of an hole validation that contains all errors that occurred during the validation. */
sealed class ValidationResult<in T> {
    /** Gets all validation failures that occurred at the specified path or as one of its sub validations. */
    abstract operator fun get(vararg propertyPath: Any): List<String>?

    /** Gets all [ValidationError] instances that occurred during the validation. */
    abstract val errors: ValidationErrors

    internal abstract fun <R> withValue(transform: () -> R): ValidationResult<R>
}

/** Represents an invalid validation result. */
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

    /** Returns a string containing all of the errors in [errors]. */
    override fun toString(): String = "Invalid(errors=${errors})"
}

/** Represents an valid validation result. */
data class Valid<T>(
    /** The object that was validated. */
    val value: T
) : ValidationResult<T>() {
    override fun get(vararg propertyPath: Any): List<String>? = null
    override fun <R> withValue(transform: () -> R): ValidationResult<R> = Valid(transform())
    override val errors: ValidationErrors get() = NoValidationErrors
}

internal data class NoResult<T>(val value: T) : ValidationResult<T>() {
    override fun get(vararg propertyPath: Any): List<String>? = null
    override fun <R> withValue(transform: () -> R): ValidationResult<R> = NoResult(transform())
    override val errors: ValidationErrors get() = NoValidationErrors
}