package de.kotlinBerlin.kValidation

import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

typealias ValidationErrors = List<ValidationError>

open class ValidationPath(val segments: List<PathDescriptor<*, *>>) {
    override fun toString(): String = segments.fold("this") { previousResult, nextSegment ->
        combineSegments(nextSegment, previousResult)
    }

    private fun combineSegments(
        nextSegment: PathDescriptor<*, *>,
        previousResult: String
    ): String {
        return when (nextSegment) {
            is PropertyPathDescriptor, is FunctionPathDescriptor -> "$previousResult.${nextSegment.name}"
            ThisPathDescriptor -> previousResult
            is MapPathDescriptor<*, *>, is IndexPathDescriptor -> "$previousResult${nextSegment.name}"
            is ConditionalPathDescriptor -> combineSegments(nextSegment.descriptor, previousResult)
        }
    }
}

object EmptyValidationPath : ValidationPath(emptyList())

interface ValidationError {
    val dataPath: String
    val path: ValidationPath
    val message: String
}

internal data class PropertyValidationError(
    override val path: ValidationPath,
    override val message: String
) : ValidationError {
    override val dataPath: String get() = path.toString()
    override fun toString(): String = "ValidationError(dataPath=$dataPath, message=$message)"

}

internal object NoValidationErrors : ValidationErrors by emptyList()
internal class DefaultValidationErrors(private val errors: List<ValidationError>) : ValidationErrors by errors {
    override fun toString(): String = errors.toString()
}

sealed class ValidationResult<in T> {
    abstract operator fun get(vararg propertyPath: Any): ErrorDescriptor?
    abstract fun <R> withValue(transform: () -> R): ValidationResult<R>
    abstract val errors: ValidationErrors
}

data class ErrorDescriptor(internal var _path: ValidationPath, val errors: List<String>) {
    val path: ValidationPath get() = _path
}

class Invalid<T>(
    internal val internalErrors: Map<String, ErrorDescriptor>
) : ValidationResult<T>() {

    init {
        internalErrors.forEach { mapEntry ->
            mapEntry.value._path =
                ValidationPath(mapEntry.value._path.segments.filter { it !is ThisPathDescriptor }
                    .map { if (it is ConditionalPathDescriptor) it.descriptor else it })
        }
    }

    override fun get(vararg propertyPath: Any): ErrorDescriptor? =
        internalErrors[propertyPath.joinToString("", transform = ::toPathSegment)]

    override fun <R> withValue(transform: () -> R): ValidationResult<R> = Invalid(this.internalErrors)

    private fun toPathSegment(it: Any): String = when (it) {
        is KFunction1<*, *> -> ".${it.name}"
        is KProperty1<*, *> -> ".${it.name}"
        is PathDescriptor<*, *> -> ".${it.name}"
        is Int -> "[$it]"
        else -> ".$it"
    }

    override val errors: ValidationErrors by lazy {
        DefaultValidationErrors(
            (internalErrors).flatMap { (_, error) ->
                error.errors.map { it -> PropertyValidationError(error.path, it) }
            }
        )
    }

    override fun toString(): String {
        return "Invalid(errors=${errors})"
    }
}

data class Valid<T>(val value: T) : ValidationResult<T>() {
    override fun get(vararg propertyPath: Any): ErrorDescriptor? = null

    override fun <R> withValue(transform: () -> R): ValidationResult<R> = Valid(transform())
    override val errors: ValidationErrors get() = NoValidationErrors
}

data class NoResult<T>(val value: T) : ValidationResult<T>() {
    override fun get(vararg propertyPath: Any): ErrorDescriptor? = null

    override fun <R> withValue(transform: () -> R): ValidationResult<R> = NoResult(transform())
    override val errors: ValidationErrors get() = NoValidationErrors
}

sealed class ValidationResultNode(val children: Collection<ValidationResultNode>)

class PathValidationResultNode(val pathDescriptor: PathDescriptor<*, *>, children: Collection<ValidationResultNode>) :
    ValidationResultNode(children)

class SimpleValidationResultNode(val result: ValidationResult<*>) : ValidationResultNode(emptyList())
