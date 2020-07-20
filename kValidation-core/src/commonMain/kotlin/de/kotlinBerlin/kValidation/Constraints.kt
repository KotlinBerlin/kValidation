package de.kotlinBerlin.kValidation

class Constraint<in R> internal constructor(
    val hint: String,
    val templateValues: List<String>,
    val test: (R) -> Boolean
)

fun <T : Any?> ValidationBuilder<T>.isNotNull(): Constraint<T> {
    return addConstraint("may not be null") { it != null }
}

fun <T : Any?> ValidationBuilder<T>.isNull(): Constraint<T> {
    return addConstraint("must be null") { it == null }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> ValidationBuilder<out Any?>.type(): Constraint<Any?> =
    addConstraint(
        "must be of the correct type"
    ) { it is T } as Constraint<Any?>

fun <T> ValidationBuilder<T>.enum(vararg allowed: T) =
    addConstraint(
        "must be one of: {0}",
        allowed.joinToString("', '", "'", "'")
    ) { it in allowed }

fun <T> ValidationBuilder<T>.const(expected: T) =
    addConstraint(
        "must be {0}",
        expected?.let { "'$it'" } ?: "null"
    ) { expected == it }