package de.kotlinBerlin.kValidation.constraints

import de.kotlinBerlin.kValidation.ValidationBuilder

class Constraint<in R> internal constructor(
    val hint: String,
    val templateValues: List<String>,
    val isError: Boolean = true,
    val test: (R, Map<String, Any?>) -> Boolean
)

fun <T : Any> ValidationBuilder<T?>.isNotNull(): Constraint<T?> =
    addConstraint("may not be null") { tempValue, _ -> tempValue != null }

fun <T : Any> ValidationBuilder<T?>.isNull(): Constraint<T?> =
    addConstraint("must be null") { tempValue, _ -> tempValue == null }

@Suppress("UNCHECKED_CAST")
inline fun <reified T> ValidationBuilder<out Any?>.type(): Constraint<Any?> =
    addConstraint("must be of the correct type") { tempValue, _ -> tempValue is T } as Constraint<Any?>

fun <T> ValidationBuilder<T>.enum(vararg allowed: T): Constraint<T> =
    addConstraint(
        "must be one of: {0}",
        allowed.joinToString("', '", "'", "'")
    ) { tempValue, _ -> tempValue in allowed }

fun <T> ValidationBuilder<T>.const(expected: T): Constraint<T> =
    addConstraint(
        "must be {0}",
        expected?.let { "'$it'" } ?: "null") { tempValue, _ -> expected == tempValue }

inline fun <T> ValidationBuilder<T>.simpleCustom(crossinline test: (T) -> Boolean): Constraint<T> =
    addConstraint("custom constraint failed") { tempValue, _ -> test(tempValue) }

inline fun <T> ValidationBuilder<T>.custom(crossinline test: (T, Map<String, Any?>) -> Boolean): Constraint<T> =
    addConstraint("custom constraint failed") { tempValue, tempContext -> test(tempValue, tempContext) }
