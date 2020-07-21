package de.kotlinBerlin.kValidation.constraints

import de.kotlinBerlin.kValidation.ValidationBuilder
import kotlin.math.roundToInt

fun <T : Number> ValidationBuilder<T>.multipleOf(factor: Number): Constraint<T> {
    val factorAsDouble = factor.toDouble()
    require(factorAsDouble > 0) { "multipleOf requires the factor to be strictly larger than 0" }
    return addConstraint("'{value}' must be a multiple of '{0}'", factor.toString()) { tempValue, _ ->
        val division = tempValue.toDouble() / factorAsDouble
        division.compareTo(division.roundToInt()) == 0
    }
}

fun <T : Number> ValidationBuilder<T>.maximum(maximumInclusive: Number) =
    addConstraint(
        "'{value}' must be at most '{0}'",
        maximumInclusive.toString()
    ) { tempValue, _ -> tempValue.toDouble() <= maximumInclusive.toDouble() }

fun <T : Number> ValidationBuilder<T>.exclusiveMaximum(maximumExclusive: Number) = addConstraint(
    "'{value}' must be less than '{0}'",
    maximumExclusive.toString()

) { tempValue, _ -> tempValue.toDouble() < maximumExclusive.toDouble() }

fun <T : Number> ValidationBuilder<T>.minimum(minimumInclusive: Number) = addConstraint(
    "'{value}' must be at least '{0}'",
    minimumInclusive.toString()
) { tempValue, _ -> tempValue.toDouble() >= minimumInclusive.toDouble() }

fun <T : Number> ValidationBuilder<T>.exclusiveMinimum(minimumExclusive: Number) = addConstraint(
    "'{value}' must be greater than '{0}'",
    minimumExclusive.toString()
) { tempValue, _ -> tempValue.toDouble() > minimumExclusive.toDouble() }

fun <T : Number> ValidationBuilder<T>.between(startInclusive: Number, endInclusive: Number) = addConstraint(
    "'{value}' must be at least '{0}' and not greater than '{1}'",
    startInclusive.toString(),
    endInclusive.toString()
) { tempValue, _ -> tempValue.toDouble() >= startInclusive.toDouble() && tempValue.toDouble() <= endInclusive.toDouble() }

fun <T : Number> ValidationBuilder<T>.betweenExclusive(startInclusive: Number, endExclusive: Number) = addConstraint(
    "'{value}' must be greater than '{0}' and less than '{1}'",
    startInclusive.toString(),
    endExclusive.toString()
) { tempValue, _ -> tempValue.toDouble() > startInclusive.toDouble() && tempValue.toDouble() < endExclusive.toDouble() }

fun <T, R> ValidationBuilder<T>.inRange(range: ClosedRange<R>): Constraint<T>
        where R : Comparable<R>,
              R : Number,
              T : Number = addConstraint(
    "'{value}' must be at least '{0}' and not greater than '{1}'",
    range.start.toString(),
    range.endInclusive.toString()
) { tempValue, _ -> tempValue.toDouble() in range.start.toDouble()..range.endInclusive.toDouble() }