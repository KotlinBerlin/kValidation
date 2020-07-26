package de.kotlinBerlin.kValidation.constraints

import de.kotlinBerlin.kValidation.ValidationBuilder

/** Checks that the boolean is true */
fun ValidationBuilder<Boolean>.isTrue(): Constraint<Boolean> =
    addConstraint("must be true") { tempValue, _ -> tempValue }

/** Checks that the boolean is false */
fun ValidationBuilder<Boolean>.isFalse(): Constraint<Boolean> =
    addConstraint("must be true") { tempValue, _ -> !tempValue }