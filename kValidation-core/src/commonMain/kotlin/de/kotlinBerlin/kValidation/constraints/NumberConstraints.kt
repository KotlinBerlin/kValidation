@file:Suppress("unused")

package de.kotlinBerlin.kValidation.constraints

import de.kotlinBerlin.kValidation.ValidationBuilder
import kotlin.jvm.JvmOverloads
import kotlin.math.roundToInt

/** Checks whether or not the number is a multiple of [factor]. */
fun <T : Number> ValidationBuilder<T>.multipleOf(factor: Number): Constraint<T> {
    val factorAsDouble = factor.toDouble()
    require(factorAsDouble > 0) { "multipleOf requires the factor to be strictly larger than 0" }
    return addConstraint("must be a multiple of '{0}'", factor.toString()) { tempValue, _ ->
        val division = tempValue.toDouble() / factorAsDouble
        division.compareTo(division.roundToInt()) == 0
    }
}

/** Checks whether or not the number is < (if [exclusive] is true) or <= (if [exclusive] is false) than [maximum]. */
@JvmOverloads
fun <T : Number> ValidationBuilder<T>.maximum(maximum: Number, exclusive: Boolean = false): Constraint<T> =
    addConstraint(
        if (exclusive) "must be less than '{0}'" else "must be at most '{0}'",
        maximum.toString()
    ) { tempValue, _ -> if (exclusive) tempValue.toDouble() < maximum.toDouble() else tempValue.toDouble() <= maximum.toDouble() }

/** Checks whether or not the number is > (if [exclusive] is true) or >= (if [exclusive] is false) than [minimum]. */
@JvmOverloads
fun <T : Number> ValidationBuilder<T>.minimum(minimum: Number, exclusive: Boolean = false): Constraint<T> =
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

/** Checks whether or not the number is in the [range]. */
fun <T, R> ValidationBuilder<T>.inRange(range: ClosedRange<R>): Constraint<T>
        where R : Comparable<R>,
              R : Number,
              T : Number = addConstraint(
    "must be at least '{0}' and not greater than '{1}'",
    range.start.toString(),
    range.endInclusive.toString()
) { tempValue, _ -> tempValue.toDouble() in range.start.toDouble()..range.endInclusive.toDouble() }