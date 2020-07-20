package de.kotlinBerlin.kValidation

import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

typealias ValidationErrors = List<ValidationError>

interface ValidationError {
    val dataPath: String
    val message: String
}

internal data class PropertyValidationError(
    override val dataPath: String,
    override val message: String
) : ValidationError {
    override fun toString(): String {
        return "ValidationError(dataPath=$dataPath, message=$message)"
    }
}

internal object NoValidationErrors : ValidationErrors by emptyList()
internal class DefaultValidationErrors(private val errors: List<ValidationError>) : ValidationErrors by errors {
    override fun toString(): String = errors.toString()
}

sealed class ValidationResult<T>() {
    abstract operator fun get(vararg propertyPath: Any): List<String>?
    abstract fun <R> map(transform: (T) -> R): ValidationResult<R>
    abstract val errors: ValidationErrors
}

class Invalid<T>(
    internal val internalErrors: Map<String, List<String>>
) : ValidationResult<T>() {

    override fun get(vararg propertyPath: Any): List<String>? =
        internalErrors[propertyPath.joinToString("", transform = ::toPathSegment)]

    override fun <R> map(transform: (T) -> R): ValidationResult<R> =
        Invalid(this.internalErrors)

    private fun toPathSegment(it: Any): String = when (it) {
        is KFunction1<*, *> -> ".${it.name}"
        is KProperty1<*, *> -> ".${it.name}"
        is PathDescriptor<*, *> -> ".${it.name}"
        is Int -> "[$it]"
        else -> ".$it"
    }

    override val errors: ValidationErrors by lazy {
        DefaultValidationErrors(
            internalErrors.flatMap { (path, errors) ->
                errors.map { PropertyValidationError(path, it) }
            }
        )
    }

    override fun toString(): String {
        return "Invalid(errors=${errors})"
    }
}

data class Valid<T>(val value: T) : ValidationResult<T>() {
    override fun get(vararg propertyPath: Any): List<String>? = null
    override fun <R> map(transform: (T) -> R): ValidationResult<R> = Valid(transform(this.value))
    override val errors: ValidationErrors get() = NoValidationErrors
}

data class NoResult<T>(val value: T) : ValidationResult<T>() {
    override fun get(vararg propertyPath: Any): List<String>? = null
    override fun <R> map(transform: (T) -> R): ValidationResult<R> = NoResult(transform(this.value))
    override val errors: ValidationErrors get() = NoValidationErrors
}

sealed class ValidationResultNode(val children: Collection<ValidationResultNode>)

class PathValidationResultNode(val pathDescriptor: PathDescriptor<*, *>, children: Collection<ValidationResultNode>) :
    ValidationResultNode(children)

class SimpleValidationResultNode(val result: ValidationResult<*>) : ValidationResultNode(emptyList())
