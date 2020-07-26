@file:Suppress("unused")

package de.kotlinBerlin.kValidation.constraints

import de.kotlinBerlin.kValidation.ValidationBuilder
import kotlin.reflect.KClass

/** A [Constraint] is a single validation on an object. */
class Constraint<in R> internal constructor(
    /** The hint used to describe the failed validation. */
    val hint: String,
    /** values available as placeholders in the [hint]. */
    val templateValues: List<String>,
    /**
     * Whether or not a failed validation should be treated
     * as an [de.kotlinBerlin.kValidation.Error] or an [de.kotlinBerlin.kValidation.Warning].
     */
    val isError: Boolean = true,
    /** The function that perform the actual check which should return true if the validation passes and false otherwise.*/
    val test: (R, Map<String, Any?>) -> Boolean
)

/** Checks that the object is not null. */
fun <T : Any> ValidationBuilder<T?>.isNotNull(): Constraint<T?> =
    addConstraint("may not be null") { tempValue, _ -> tempValue != null }

/** Checks that the object is null. */
fun <T : Any> ValidationBuilder<T?>.isNull(): Constraint<T?> =
    addConstraint("must be null") { tempValue, _ -> tempValue == null }

/** Checks that the object is of the specified type. */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> ValidationBuilder<out Any?>.type(): Constraint<Any?> =
    addConstraint("must be of the correct type") { tempValue, _ -> tempValue is T } as Constraint<Any?>

/** Checks that the object is of the specified type. */
@Suppress("UNCHECKED_CAST")
fun <T : Any> ValidationBuilder<out Any?>.type(aKotlinClass: KClass<T>): Constraint<Any?> =
    addConstraint("must be of the correct type") { tempValue, _ -> aKotlinClass.isInstance(tempValue) } as Constraint<Any?>

/** Checks that the object is one of the [allowed] values. Comparison is done via the [Any.equals] method. */
fun <T> ValidationBuilder<T>.oneOf(vararg allowed: T): Constraint<T> =
    addConstraint(
        "must be one of: {0}",
        allowed.joinToString("', '", "'", "'")
    ) { tempValue, _ -> tempValue in allowed }

/** Checks that the object is the same as [expected]. Comparison is done via the [Any.equals] method. */
fun <T> ValidationBuilder<T>.const(expected: T): Constraint<T> =
    addConstraint(
        "must be {0}",
        expected?.let { "'$it'" } ?: "null") { tempValue, _ -> expected == tempValue }

/** Defines a [Constraint] with a custom check that does not need the context values. */
inline fun <T> ValidationBuilder<T>.simpleCustom(
    aMessage: String = "custom constraint failed",
    crossinline test: (T) -> Boolean
): Constraint<T> = custom(aMessage) { tempValue, _ -> test(tempValue) }

/** Defines a [Constraint] with a custom check. */
inline fun <T> ValidationBuilder<T>.custom(
    aMessage: String = "custom constraint failed",
    crossinline test: (T, Map<String, Any?>) -> Boolean
): Constraint<T> =
    addConstraint(aMessage) { tempValue, tempContext -> test(tempValue, tempContext) }

/** Checks whether or not the object is in the [range]. */
fun <T> ValidationBuilder<T>.inRange(range: ClosedRange<T>): Constraint<T>
        where T : Comparable<T> = addConstraint(
    "must be at least '{0}' and not greater than '{1}'",
    range.start.toString(),
    range.endInclusive.toString()
) { tempValue, _ -> tempValue in range }
