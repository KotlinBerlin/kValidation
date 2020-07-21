package de.kotlinBerlin.kValidation.internal

import de.kotlinBerlin.kValidation.*

internal class UndefinedPropertyValidation<T, R>(
    private val pathDescriptor: PathDescriptor<T, R>,
    private val validation: Validation<R>
) : Validation<T> {
    override fun validate(aValue: T, aContext: ValidationContext): ValidationResult<T> {
        if (pathDescriptor is ConditionalPathDescriptor) {
            val tempResult = pathDescriptor.condition.validate(aValue, aContext)
            if (tempResult !is Valid) return Valid(aValue)

        }
        val propertyValue = pathDescriptor[aValue]
        return validation.validate(propertyValue, aContext).mapError(pathDescriptor) { ".${pathDescriptor.name}$it" }
            .withValue { aValue }
    }
}

internal class OptionalPropertyValidation<T, R>(
    private val pathDescriptor: PathDescriptor<T, R>,
    private val validation: Validation<R>
) : Validation<T> {
    override fun validate(aValue: T, aContext: ValidationContext): ValidationResult<T> {
        if (pathDescriptor is ConditionalPathDescriptor) {
            val tempResult = pathDescriptor.condition.validate(aValue, aContext)
            if (tempResult !is Valid) return Valid(aValue)

        }
        val propertyValue = pathDescriptor[aValue] ?: return Valid(aValue)
        return validation.validate(propertyValue, aContext).mapError(pathDescriptor) { ".${pathDescriptor.name}$it" }
            .withValue { aValue }
    }
}

internal class RequiredPropertyValidation<T, R>(
    private val pathDescriptor: PathDescriptor<T, R>,
    private val validation: Validation<R>
) : Validation<T> {
    override fun validate(aValue: T, aContext: ValidationContext): ValidationResult<T> {
        if (pathDescriptor is ConditionalPathDescriptor) {
            val tempResult = pathDescriptor.condition.validate(aValue, aContext)
            if (tempResult !is Valid) return Valid(aValue)
        }
        val propertyValue = pathDescriptor[aValue]
            ?: return Invalid(
                mapOf(
                    ".${pathDescriptor.name}" to ErrorDescriptor(
                        ValidationPath(listOf(pathDescriptor)),
                        listOf("is required")
                    )
                )
            )
        return validation.validate(propertyValue, aContext).mapError(pathDescriptor) { ".${pathDescriptor.name}${it}" }
            .withValue { aValue }
    }
}

internal class IterableValidation<T>(
    private val validation: Validation<T>
) : Validation<Iterable<T>> {
    override fun validate(aValue: Iterable<T>, aContext: ValidationContext): ValidationResult<Iterable<T>> {
        return aValue.foldIndexed(Valid(aValue)) { index, result: ValidationResult<Iterable<T>>, propertyValue ->
            val propertyValidation =
                validation.validate(propertyValue, aContext)
                    .mapError(IndexPathDescriptor(index, propertyValue)) { "[$index]$it" }.withValue { aValue }
            val tempCombinedResult = result.combineWith(propertyValidation, false)
            tempCombinedResult
        }
    }
}

internal class ArrayValidation<T>(
    private val validation: Validation<T>
) : Validation<Array<T>> {
    override fun validate(aValue: Array<T>, aContext: ValidationContext): ValidationResult<Array<T>> {
        return aValue.foldIndexed(Valid(aValue)) { index, result: ValidationResult<Array<T>>, propertyValue ->
            val propertyValidation =
                validation.validate(propertyValue, aContext)
                    .mapError(IndexPathDescriptor(index, propertyValue)) { "[$index]$it" }
                    .withValue { aValue }
            val tempCombinedResult = result.combineWith(propertyValidation, false)
            tempCombinedResult
        }
    }
}

internal class MapValidation<K, V>(
    private val validation: Validation<Map.Entry<K, V>>
) : Validation<Map<K, V>> {
    override fun validate(aValue: Map<K, V>, aContext: ValidationContext): ValidationResult<Map<K, V>> {
        return aValue.entries.fold(Valid(aValue)) { result: ValidationResult<Map<K, V>>, entry ->
            val propertyValidation =
                validation.validate(entry, aContext)
                    .mapError(MapPathDescriptor(entry)) { ".${entry.key.toString()}${it.removePrefix(".value")}" }
                    .withValue { aValue }
            val tempCombinedResult = result.combineWith(propertyValidation, false)
            tempCombinedResult
        }
    }
}

internal class ValidationNode<T>(
    private val constraints: List<Constraint<T>>,
    private val subValidations: List<Validation<T>>,
    private val combineWithOr: Boolean = false,
    private val shortCircuit: Boolean = false
) : Validation<T> {
    override fun validate(aValue: T, aContext: ValidationContext): ValidationResult<T> =
        if (combineWithOr) validateOr(aValue, aContext) else validateAnd(aValue, aContext)

    private fun validateOr(aValue: T, aContext: ValidationContext): ValidationResult<T> {
        val tempLocalValidationResult: ValidationResult<T> =
            if (constraints.isEmpty()) NoResult(aValue) else validateLocalOr(aValue, aContext)

        if (shortCircuit && tempLocalValidationResult is Valid) return tempLocalValidationResult

        val tempSubValidationResult = validateSubOr(aValue, aContext)

        if (shortCircuit && tempSubValidationResult is Valid) return tempSubValidationResult

        return tempLocalValidationResult.combineWith(tempSubValidationResult, true)
    }

    private fun validateSubOr(aValue: T, aContext: ValidationContext): ValidationResult<T> {
        return subValidations.fold(NoResult(aValue)) { existingValidation: ValidationResult<T>, validation ->
            val newValidation = validation.validate(aValue, aContext)
            val tempCombinedResult = existingValidation.combineWith(newValidation, true)
            if (tempCombinedResult is Valid && shortCircuit) {
                return tempCombinedResult
            } else {
                tempCombinedResult
            }
        }
    }

    private fun validateLocalOr(aValue: T, aContext: ValidationContext): ValidationResult<T> {
        val tempLocalValidationResultPairs = constraints
            .map {
                val tempResult = it.test(aValue, aContext)
                if (tempResult && shortCircuit) {
                    return Valid(aValue)
                } else {
                    Pair(it, tempResult)
                }
            }
        return if (tempLocalValidationResultPairs.any { it.second }) {
            Valid(aValue)
        } else {
            tempLocalValidationResultPairs.map { it.first }.map { constructHint(aValue, it) }
                .let {
                    if (it.isEmpty()) {
                        Valid(aValue)
                    } else {
                        Invalid(mapOf("" to ErrorDescriptor(EmptyValidationPath, it)))
                    }
                }
        }
    }

    private fun validateAnd(aValue: T, aContext: ValidationContext): ValidationResult<T> {
        val tempLocalValidationResult =
            if (constraints.isEmpty()) NoResult(aValue) else validateLocalAnd(aValue, aContext)

        if (shortCircuit && tempLocalValidationResult is Invalid) return tempLocalValidationResult

        val tempSubValidationResult = validateSubAnd(aValue, aContext)

        if (shortCircuit && tempSubValidationResult is Invalid) return tempSubValidationResult

        return tempLocalValidationResult.combineWith(tempSubValidationResult, false)
    }

    private fun validateSubAnd(aValue: T, aContext: ValidationContext): ValidationResult<T> {
        return subValidations.fold(NoResult(aValue)) { existingValidation: ValidationResult<T>, validation ->
            val newValidation = validation.validate(aValue, aContext)
            val tempCombinedResult = existingValidation.combineWith(newValidation, false)
            if (tempCombinedResult is Invalid && shortCircuit) {
                return tempCombinedResult
            } else {
                tempCombinedResult
            }
        }
    }

    private fun validateLocalAnd(aValue: T, aContext: ValidationContext): ValidationResult<T> {
        return constraints
            .filter {
                val tempResult = it.test(aValue, aContext)
                if (!tempResult && shortCircuit) {
                    return Invalid(
                        mapOf(
                            "" to ErrorDescriptor(
                                EmptyValidationPath,
                                listOf(constructHint(aValue, it))
                            )
                        )
                    )
                } else {
                    !tempResult
                }
            }
            .map { constructHint(aValue, it) }
            .let {
                if (it.isEmpty()) {
                    Valid(aValue)
                } else {
                    Invalid(mapOf("" to ErrorDescriptor(EmptyValidationPath, it)))
                }
            }
    }

    private fun constructHint(value: T, it: Constraint<T>): String {
        val replaceValue = it.hint.replace("{value}", value.toString())
        return it.templateValues
            .foldIndexed(replaceValue) { index, hint, templateValue -> hint.replace("{$index}", templateValue) }
    }
}

internal fun <R> ValidationResult<R>.mapError(
    aPathDescriptor: PathDescriptor<*, R>,
    keyTransform: (String) -> String
): ValidationResult<R> {
    return when (this) {
        is Valid -> this
        is NoResult -> this
        is Invalid -> {
            val tempMapped = this.internalErrors.map {
                val tempPath = it.value.path.segments
                val tempErrors = it.value.errors
                Pair(
                    keyTransform(it.key),
                    ErrorDescriptor(ValidationPath(listOf(aPathDescriptor, *tempPath.toTypedArray())), tempErrors)
                )
            }.toMap()
            Invalid(tempMapped)
        }
    }
}

internal fun <R> ValidationResult<R>.combineWith(
    other: ValidationResult<R>,
    aCombineWithOrFlag: Boolean
): ValidationResult<R> {
    return when (this) {
        is NoResult -> other
        is Valid -> when {
            other is NoResult || aCombineWithOrFlag -> this
            else -> other
        }
        is Invalid -> when (other) {
            is NoResult -> this
            is Valid -> when {
                this is NoResult || aCombineWithOrFlag -> other
                else -> this
            }
            is Invalid -> {
                val tempCombinedErrors = this.internalErrors.toList() + other.internalErrors.toList()
                val tempGroupedValues = tempCombinedErrors.groupBy({ it.first }) { it.second }
                val tempMappedValues = tempGroupedValues.mapValues { (_, descriptors) ->
                    ErrorDescriptor(
                        descriptors.first().path,
                        descriptors.flatMap { it.errors })
                }
                Invalid(tempMappedValues)
            }
        }
    }
}
