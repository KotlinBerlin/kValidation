package de.kotlinBerlin.kValidation.internal

import de.kotlinBerlin.kValidation.*
import de.kotlinBerlin.kValidation.constraints.Constraint

internal class UndefinedPropertyValidation<T, R>(
    private val pathDescriptor: PathDescriptor<T, R>,
    private val validation: Validation<R>
) : Validation<T>() {
    @Suppress("DuplicatedCode")
    override fun validate(aValue: T, aContext: ValidationContext<*>): Boolean {
        if (pathDescriptor is ConditionalPathDescriptor) {
            if (!pathDescriptor.condition.validate(aValue, aContext.copyForCondition(aValue))) return true
        }

        val tempValue = pathDescriptor[aValue]
        aContext.push { PathResult(pathDescriptor, tempValue, it) }
        return if (validation.validate(tempValue, aContext)) {
            aContext.pop()
            true
        } else {
            aContext.popAndAddToParent()
            false
        }
    }
}

internal class OptionalPropertyValidation<T, R>(
    private val pathDescriptor: PathDescriptor<T, R>,
    private val validation: Validation<R>
) : Validation<T>() {
    @Suppress("DuplicatedCode")
    override fun validate(aValue: T, aContext: ValidationContext<*>): Boolean {
        if (pathDescriptor is ConditionalPathDescriptor) {
            if (!pathDescriptor.condition.validate(aValue, aContext.copyForCondition(aValue))) return true
        }
        val tempValue = pathDescriptor[aValue] ?: return true
        aContext.push { PathResult(pathDescriptor, tempValue, it) }
        return if (validation.validate(tempValue, aContext)) {
            aContext.pop()
            true
        } else {
            aContext.popAndAddToParent()
            false
        }
    }
}

internal class RequiredPropertyValidation<T, R>(
    private val pathDescriptor: PathDescriptor<T, R>,
    private val validation: Validation<R>
) : Validation<T>() {
    @Suppress("DuplicatedCode")
    override fun validate(aValue: T, aContext: ValidationContext<*>): Boolean {
        if (pathDescriptor is ConditionalPathDescriptor) {
            if (!pathDescriptor.condition.validate(aValue, aContext.copyForCondition(aValue))) return true
        }
        val tempValue = pathDescriptor[aValue]
        aContext.push { PathResult(pathDescriptor, tempValue, it) }
        return if (tempValue == null) {
            aContext.push { AndResult(tempValue, it) }
            aContext.addInvalidResult { Error<R?>("is required", null, it) }
            aContext.popAndAddToParent()
            aContext.popAndAddToParent()
            false
        } else {
            if (validation.validate(tempValue, aContext)) {
                aContext.pop()
                true
            } else {
                aContext.popAndAddToParent()
                false
            }
        }
    }
}

internal class IterableValidation<T>(
    private val validation: Validation<T>,
    private vararg val anIndexList: Int
) : Validation<Iterable<T>>() {
    override fun validate(aValue: Iterable<T>, aContext: ValidationContext<*>): Boolean {
        aContext.push { AndResult(aValue, it) }
        val tempResult = aValue.foldIndexed(true) { tempIndex, tempPreviousResult, tempValue ->
            if (anIndexList.isNotEmpty() && !anIndexList.contains(tempIndex)) return@foldIndexed tempPreviousResult
            val tempDescriptor = IterablePathDescriptor<T>(tempIndex)
            aContext.push { PathResult(tempDescriptor, tempValue, it) }
            if (validation.validate(tempValue, aContext)) {
                aContext.pop()
                tempPreviousResult
            } else {
                aContext.popAndAddToParent()
                false
            }
        }
        return if (tempResult) {
            aContext.pop()
            true
        } else {
            aContext.popAndAddToParent()
            false
        }
    }
}

internal class ArrayValidation<T>(
    private val validation: Validation<T>,
    private vararg val anIndexList: Int
) : Validation<Array<T>>() {
    override fun validate(aValue: Array<T>, aContext: ValidationContext<*>): Boolean {
        aContext.push { AndResult(aValue, it) }
        val tempResult = aValue.foldIndexed(true) { tempIndex, tempPreviousResult, tempValue ->
            if (anIndexList.isNotEmpty() && !anIndexList.contains(tempIndex)) return@foldIndexed tempPreviousResult
            val tempDescriptor = ArrayPathDescriptor<T>(tempIndex)
            aContext.push { PathResult(tempDescriptor, tempValue, it) }
            if (validation.validate(tempValue, aContext)) {
                aContext.pop()
                tempPreviousResult
            } else {
                aContext.popAndAddToParent()
                false
            }
        }
        return if (tempResult) {
            aContext.pop()
            true
        } else {
            aContext.popAndAddToParent()
            false
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal class MapValidation<K, V>(
    private val validation: Validation<Map.Entry<K, V>>,
    private vararg val aKeyList: K
) : Validation<Map<K, V>>() {
    override fun validate(aValue: Map<K, V>, aContext: ValidationContext<*>): Boolean {
        aContext.push { AndResult(aValue, it) }
        val tempResult = aValue.entries.fold(true) { tempPreviousResult, tempValue ->
            if (aKeyList.isNotEmpty() && !aKeyList.contains(tempValue.key)) return@fold tempPreviousResult
            val tempDescriptor = MapPathDescriptor<K, V>(tempValue.key)
            aContext.push { PathResult(tempDescriptor, tempValue, it) }
            if (validation.validate(tempValue, aContext)) {
                aContext.pop()
                tempPreviousResult
            } else {
                aContext.popAndAddToParent()
                false
            }
        }
        return if (tempResult) {
            aContext.pop()
            true
        } else {
            aContext.popAndAddToParent()
            false
        }
    }
}

internal class ObjectValidation<T>(
    private val constraints: List<Constraint<T>>,
    private val subValidations: List<Validation<in T>>,
    private val combineWithOr: Boolean,
    private val shortCircuit: Boolean,
) : Validation<T>() {
    override fun validate(aValue: T, aContext: ValidationContext<*>): Boolean {
        aContext.push {
            if (combineWithOr) {
                OrResult(aValue, it)
            } else {
                AndResult(aValue, it)
            }
        }
        return if (validateIntern(aValue, aContext)) {
            aContext.pop()
            true
        } else {
            aContext.popAndAddToParent()
            false
        }
    }

    private fun validateIntern(aValue: T, aContext: ValidationContext<*>): Boolean =
        if (combineWithOr) validateOrIntern(aValue, aContext) else validateAndIntern(aValue, aContext)

    private fun validateAndIntern(aValue: T, aContext: ValidationContext<*>): Boolean {
        val tempConstraintResult = constraints.fold(null) { tempPreviousResult: Boolean?, tempConstraint ->
            if (tempConstraint.test(aValue, aContext)) {
                tempPreviousResult ?: true
            } else {
                aContext.addInvalidResult { constructInvalidResult(tempConstraint, aValue, it) }
                if (shortCircuit) return false
                false
            }
        }

        val tempSubValidationsResult = subValidations.fold(null) { tempPreviousResult: Boolean?, tempValidation ->
            when {
                tempValidation.validate(aValue, aContext) -> {
                    tempPreviousResult ?: true
                }
                shortCircuit -> return false
                else -> false
            }
        }

        return if (tempConstraintResult != null && tempSubValidationsResult != null) {
            tempConstraintResult && tempSubValidationsResult
        } else {
            tempConstraintResult ?: (tempSubValidationsResult ?: true)
        }
    }

    private fun validateOrIntern(aValue: T, aContext: ValidationContext<*>): Boolean {
        val tempConstraintResult = constraints.fold(null) { tempPreviousResult: Boolean?, tempConstraint ->
            if (tempConstraint.test(aValue, aContext)) {
                if (shortCircuit) return true
                true
            } else {
                aContext.addInvalidResult { constructInvalidResult(tempConstraint, aValue, it) }
                tempPreviousResult ?: false
            }
        }

        val tempSubValidationsResult = subValidations.fold(null) { tempPreviousResult: Boolean?, tempValidation ->
            when {
                !tempValidation.validate(aValue, aContext) -> {
                    tempPreviousResult ?: false
                }
                shortCircuit -> return true
                else -> true
            }
        }

        return if (tempConstraintResult != null && tempSubValidationsResult != null) {
            tempConstraintResult || tempSubValidationsResult
        } else {
            tempConstraintResult ?: (tempSubValidationsResult ?: true)
        }
    }

    private fun constructInvalidResult(aConstraint: Constraint<T>, aValue: T, aParent: CompoundResult<*>): Invalid<T> =
        if (aConstraint.isError) {
            Error(constructHint(aValue, aConstraint), aValue, aParent)
        } else {
            Warning(constructHint(aValue, aConstraint), aValue, aParent)
        }

    private fun constructHint(value: T, aConstraint: Constraint<T>): String {
        val replaceValue = aConstraint.hint.replace("{value}", value.toString())
        return aConstraint.templateValues
            .foldIndexed(replaceValue) { index, hint, templateValue -> hint.replace("{$index}", templateValue) }
    }
}

private fun ValidationContext<*>.copyForCondition(aValue: Any?) = ValidationContext(aValue, this)
