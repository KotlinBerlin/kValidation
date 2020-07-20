package de.kotlinBerlin.kValidation.internal

import de.kotlinBerlin.kValidation.*

internal class NonNullPropertyValidation<T, R>(
    private val property: PathDescriptor<T, R>,
    private val validation: Validation<R>
) : Validation<T> {
    override fun validate(aValue: T): ValidationResult<T> {
        if (property is ConditionalPathDescriptor) {
            val tempResult = property.condition(aValue)
            if (tempResult !is Valid) return Valid(aValue)

        }
        val propertyValue = property(aValue)
        return validation.validate(propertyValue).mapError { ".${property.name}$it" }.map { aValue }
    }
}

internal class OptionalPropertyValidation<T, R>(
    private val property: PathDescriptor<T, R?>,
    private val validation: Validation<R>
) : Validation<T> {
    override fun validate(aValue: T): ValidationResult<T> {
        if (property is ConditionalPathDescriptor) {
            val tempResult = property.condition(aValue)
            if (tempResult !is Valid) return Valid(aValue)

        }
        val propertyValue = property(aValue) ?: return Valid(aValue)
        return validation(propertyValue).mapError { ".${property.name}$it" }.map { aValue }
    }
}

internal class RequiredPropertyValidation<T, R>(
    private val property: PathDescriptor<T, R?>,
    private val validation: Validation<R>
) : Validation<T> {
    override fun validate(aValue: T): ValidationResult<T> {
        if (property is ConditionalPathDescriptor) {
            val tempResult = property.condition(aValue)
            if (tempResult !is Valid) return Valid(aValue)
        }
        val propertyValue = property(aValue)
            ?: return Invalid(mapOf(".${property.name}" to listOf("is required")))
        return validation(propertyValue).mapError { ".${property.name}${it}" }.map { aValue }
    }
}

internal class IterableValidation<T>(
    private val validation: Validation<T>
) : Validation<Iterable<T>> {
    override fun validate(aValue: Iterable<T>): ValidationResult<Iterable<T>> {
        return aValue.foldIndexed(Valid(aValue)) { index, result: ValidationResult<Iterable<T>>, propertyValue ->
            val propertyValidation = validation(propertyValue).mapError { "[$index]$it" }.map { aValue }
            result.combineWith(propertyValidation, false)
        }
    }
}

internal class ArrayValidation<T>(
    private val validation: Validation<T>
) : Validation<Array<T>> {
    override fun validate(aValue: Array<T>): ValidationResult<Array<T>> {
        return aValue.foldIndexed(Valid(aValue)) { index, result: ValidationResult<Array<T>>, propertyValue ->
            val propertyValidation = validation(propertyValue).mapError { "[$index]$it" }.map { aValue }
            result.combineWith(propertyValidation, false)
        }
    }
}

internal class MapValidation<K, V>(
    private val validation: Validation<Map.Entry<K, V>>
) : Validation<Map<K, V>> {
    override fun validate(aValue: Map<K, V>): ValidationResult<Map<K, V>> {
        return aValue.asSequence().fold(Valid(aValue)) { result: ValidationResult<Map<K, V>>, entry ->
            val propertyValidation =
                validation(entry).mapError { ".${entry.key.toString()}${it.removePrefix(".value")}" }.map { aValue }
            result.combineWith(propertyValidation, false)
        }
    }
}

internal class ValidationNode<T>(
    private val constraints: List<Constraint<T>>,
    private val subValidations: List<Validation<T>>,
    private val combineWithOr: Boolean = false,
    private val shortCircuit: Boolean = false,
    private val customErrorMessage: String? = null
) : Validation<T> {
    override fun validate(aValue: T): ValidationResult<T> =
        if (combineWithOr) validateOr(aValue) else validateAnd(aValue)

    private fun validateOr(aValue: T): ValidationResult<T> {
        val tempLocalValidationResult = if (constraints.isEmpty()) NoResult(aValue) else validateLocalOr(aValue)

        if (shortCircuit && tempLocalValidationResult is Valid) return tempLocalValidationResult

        val tempSubValidationResult = validateSubOr(aValue)

        if (shortCircuit && tempSubValidationResult is Valid) return tempSubValidationResult

        return tempLocalValidationResult.combineWith(tempSubValidationResult, true)
    }

    private fun validateSubOr(aValue: T): ValidationResult<T> {
        return subValidations.fold(NoResult(aValue)) { existingValidation: ValidationResult<T>, validation ->
            val newValidation = validation.validate(aValue)
            val tempCombinedResult = existingValidation.combineWith(newValidation, true)
            if (tempCombinedResult is Valid && shortCircuit) {
                return tempCombinedResult
            } else {
                tempCombinedResult
            }
        }
    }

    private fun validateLocalOr(aValue: T): ValidationResult<T> {
        val tempLocalValidationResultPairs = constraints
            .map {
                val tempResult = it.test(aValue)
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
                        Invalid(mapOf("" to it))
                    }
                }
        }
    }

    private fun validateAnd(aValue: T): ValidationResult<T> {
        val tempLocalValidationResult = if (constraints.isEmpty()) NoResult(aValue) else validateLocalAnd(aValue)

        if (shortCircuit && tempLocalValidationResult is Invalid) return tempLocalValidationResult

        val tempSubValidationResult = validateSubAnd(aValue)

        if (shortCircuit && tempSubValidationResult is Invalid) return tempSubValidationResult

        return tempLocalValidationResult.combineWith(tempSubValidationResult, false)
    }

    private fun validateSubAnd(aValue: T): ValidationResult<T> {
        return subValidations.fold(NoResult(aValue)) { existingValidation: ValidationResult<T>, validation ->
            val newValidation = validation.validate(aValue)
            val tempCombinedResult = existingValidation.combineWith(newValidation, false)
            if (tempCombinedResult is Invalid && shortCircuit) {
                return tempCombinedResult
            } else {
                tempCombinedResult
            }
        }
    }

    private fun validateLocalAnd(aValue: T): ValidationResult<T> {
        return constraints
            .filter {
                val tempResult = it.test(aValue)
                if (!tempResult && shortCircuit) {
                    return Invalid(mapOf("" to listOf(constructHint(aValue, it))))
                } else {
                    !tempResult
                }
            }
            .map { constructHint(aValue, it) }
            .let {
                if (it.isEmpty()) {
                    Valid(aValue)
                } else {
                    Invalid(mapOf("" to it))
                }
            }
    }

    private fun constructHint(value: T, it: Constraint<T>): String {
        val replaceValue = it.hint.replace("{value}", value.toString())
        return it.templateValues
            .foldIndexed(replaceValue) { index, hint, templateValue -> hint.replace("{$index}", templateValue) }
    }
}

internal fun <R> ValidationResult<R>.mapError(keyTransform: (String) -> String): ValidationResult<R> {
    return when (this) {
        is Valid -> this
        is NoResult -> this
        is Invalid -> Invalid(this.internalErrors.mapKeys { (key, _) ->
            keyTransform(key)
        })
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
                val tempMappedValues = tempGroupedValues.mapValues { (_, values) -> values.flatten() }
                Invalid(tempMappedValues)
            }
        }
    }
}
