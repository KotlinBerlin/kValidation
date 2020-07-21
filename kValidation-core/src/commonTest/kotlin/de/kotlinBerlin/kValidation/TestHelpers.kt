package de.kotlinBerlin.kValidation

fun <T> countFieldsWithErrors(validationResult: ValidationResult<T>) = (validationResult as Invalid).internalErrors.size
fun countErrors(validationResult: ValidationResult<*>, vararg properties: Any) = validationResult.get(*properties)?.size
    ?: 0
