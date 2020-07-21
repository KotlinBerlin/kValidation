package de.kotlinBerlin.kValidation.kModel

import de.kotlinBerlin.kValidation.BasicValidationContext
import de.kotlinBerlin.kValidation.ValidationBuilder
import de.kotlinBerlin.kValidation.WrappingValidationContext
import de.kotlinBerlin.kValidation.constraints.Constraint

internal class NonRepeatingContext : BasicValidationContext() {

    private val visitedObjects = mutableSetOf<Any>()

    fun addVisitedObject(aVisitedObject: Any) = visitedObjects.add(aVisitedObject)
}

internal fun <T : Any> ValidationBuilder<T>.noRepeat(): Constraint<T> {
    return addConstraint("object was already validated!") { tempValue, tempCtx ->
        val tempNonRepeatingContext: NonRepeatingContext? = when {
            tempCtx is NonRepeatingContext -> tempCtx
            tempCtx is WrappingValidationContext && tempCtx.wrapped is NonRepeatingContext -> tempCtx.wrapped as NonRepeatingContext
            else -> null
        }
        when (tempNonRepeatingContext) {
            null -> true
            else -> tempNonRepeatingContext.addVisitedObject(tempValue)
        }
    }
}