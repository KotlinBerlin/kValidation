package de.kotlinBerlin.kValidation.internal

import de.kotlinBerlin.kValidation.Validation
import de.kotlinBerlin.kValidation.ValidationBuilder
import de.kotlinBerlin.kValidation.constraints.Constraint

internal abstract class BasicValidationBuilder<T>(
    protected var shortCircuit: Boolean
) : ValidationBuilder<T> {

    private val constraints = mutableListOf<Constraint<T>>()
    private val subValidations = mutableMapOf<PropKey<T>, ValidationBuilder<*>>()
    private val prebuiltValidations = mutableListOf<Validation<in T>>()

    protected abstract fun isCombineWithOr(): Boolean

    override fun build(): Validation<T> {
        val nestedValidations: List<Validation<T>> = subValidations.map { (key, builder) -> key.build(builder) }
        return ObjectValidation(
            constraints,
            nestedValidations + prebuiltValidations,
            combineWithOr = isCombineWithOr(),
            shortCircuit = shortCircuit
        )
    }

    override fun Constraint<T>.hint(aNewHint: String): Constraint<T> =
        Constraint(aNewHint, templateValues, test = test).also {
            constraints.remove(this)
            constraints.add(it)
        }

    override fun Constraint<T>.asWarning(): Constraint<T> =
        Constraint(hint, templateValues, false, test).also {
            constraints.remove(this)
            constraints.add(it)
        }

    override fun addConstraint(
        errorMessage: String,
        vararg templateValues: String,
        test: (T, Map<String, Any?>) -> Boolean
    ): Constraint<T> = Constraint(errorMessage, templateValues.toList(), test = test).also { constraints.add(it) }

    override fun run(validation: Validation<in T>) {
        prebuiltValidations.add(validation)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <R> getOrCreateBuilder(
        aKey: PropKey<T>,
        aCreator: () -> BasicValidationBuilder<R>
    ): BasicValidationBuilder<R> {
        return subValidations.getOrPut(aKey, aCreator) as BasicValidationBuilder<R>
    }
}