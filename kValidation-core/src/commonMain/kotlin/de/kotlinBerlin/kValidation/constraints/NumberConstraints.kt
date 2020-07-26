@file:Suppress("unused")

package de.kotlinBerlin.kValidation.constraints

import de.kotlinBerlin.kValidation.ValidationBuilder
import kotlin.jvm.JvmOverloads

/** Checks whether or not the number is < (if [exclusive] is true) or <= (if [exclusive] is false) than [maximum]. */
@JvmOverloads
fun <T : Number> ValidationBuilder<T>.max(maximum: Number, exclusive: Boolean = false): Constraint<T> =
    addConstraint(
        if (exclusive) "must be less than '{0}'" else "must be at most '{0}'",
        maximum.toString()
    ) { tempValue, _ -> if (exclusive) tempValue.toDouble() < maximum.toDouble() else tempValue.toDouble() <= maximum.toDouble() }

/** Checks whether or not the number is > (if [exclusive] is true) or >= (if [exclusive] is false) than [minimum]. */
@JvmOverloads
fun <T : Number> ValidationBuilder<T>.min(minimum: Number, exclusive: Boolean = false): Constraint<T> =
    addConstraint(
        if (exclusive) "must be greater than '{0}'" else "must be at least '{0}'",
        minimum.toString()
    ) { tempValue, _ -> if (exclusive) tempValue.toDouble() > minimum.toDouble() else tempValue.toDouble() >= minimum.toDouble() }

/** Checks whether or not the number is > [start] and < [end] (if [exclusive] is true) or >= [start] and <= [end] (if [exclusive] is false). */
@JvmOverloads
fun <T : Number> ValidationBuilder<T>.between(start: Number, end: Number, exclusive: Boolean = false): Constraint<T> =
    addConstraint(
        if (exclusive) "must be greater than '{0}' and less than '{1}'" else "must be at least '{0}' and not greater than '{1}'",
        start.toString(),
        end.toString()
    ) { tempValue, _ ->
        if (exclusive)
            tempValue.toDouble() > start.toDouble() && tempValue.toDouble() < end.toDouble()
        else
            tempValue.toDouble() >= start.toDouble() && tempValue.toDouble() <= end.toDouble()
    }

/** Checks whether or not the number is positive (including 0 if [allowZero] is true). */
fun <T : Number> ValidationBuilder<T>.positive(allowZero: Boolean = false): Constraint<T> =
    min(0, !allowZero) hint "must be positive${if (allowZero) "or 0" else ""}"

/** Checks whether or not the number is negative (including 0 if [allowZero] is true). */
fun <T : Number> ValidationBuilder<T>.negative(allowZero: Boolean = false): Constraint<T> =
    max(0, !allowZero) hint "must be negative${if (allowZero) "or 0" else ""}"