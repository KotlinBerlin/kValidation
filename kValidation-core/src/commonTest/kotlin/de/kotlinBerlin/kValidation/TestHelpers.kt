@file:Suppress("unused")

package de.kotlinBerlin.kValidation

import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun countFieldsWithErrors(validationResult: ValidationResult<*>): Int =
    (validationResult as Invalid<*>).groupedErrors.size

fun countErrors(validationResult: ValidationResult<*>, vararg properties: Any): Int =
    if (validationResult is Invalid<*>) validationResult.errorsAt(*properties, includeSubErrors = true).size else 0

inline fun <reified T> assertType(anObject: Any?, typedCheck: (T) -> Unit) {
    assertTrue("object should be of type: " + T::class.simpleName) { anObject is T }
    typedCheck.invoke(anObject as T)
}

fun checkMessage(
    anExpectedMessage: String,
    aResult: SimpleInvalidResult<*>
): Unit = assertEquals(anExpectedMessage, aResult.print())

