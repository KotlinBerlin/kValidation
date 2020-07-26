@file:Suppress("UNCHECKED_CAST", "unused")

package de.kotlinBerlin.kValidation.kModel

import de.kotlinBerlin.kModel.ModelAttribute
import de.kotlinBerlin.kModel.ModelClass
import de.kotlinBerlin.kModel.ModelElement
import de.kotlinBerlin.kModel.ModelRelation
import de.kotlinBerlin.kModel.dsl.ModelAttributeBuilder
import de.kotlinBerlin.kModel.dsl.ModelClassBuilder
import de.kotlinBerlin.kModel.dsl.ModelRelationBuilder
import de.kotlinBerlin.kModel.dsl.ModelReverseRelationBuilder
import de.kotlinBerlin.kValidation.AndValidationBuilder
import de.kotlinBerlin.kValidation.Validation

/** Key under which the validation is stored in the properties of the [ModelElement]*/
const val VALIDATOR_PROPERTY: String = "kValidator"

/** Returns the validation associated with this [ModelClass] or null if there is no validation defined. */
inline val <T : Any> ModelClass<T>.validation: Validation<T>?
    get() = this.modelProperties[VALIDATOR_PROPERTY] as? Validation<T>

/** Returns a validation that combines the validation for this [ModelClass] and all of its attributes and relations validations. */
val <T : Any> ModelClass<T>.fullValidation: Validation<T> get() = getFullValidation()

/** Returns the validation associated with this [ModelAttribute] or null if there is no validation defined. */
inline val <T : Any, V> ModelAttribute<T, V>.validation: Validation<V>?
    get() = this.modelProperties[VALIDATOR_PROPERTY] as? Validation<V>

/** Returns the validation associated with this [ModelRelation] or null if there is no validation defined. */
inline val <TP> ModelRelation<*, *, *, TP, *, *>.validation: Validation<TP>?
    get() = this.modelProperties[VALIDATOR_PROPERTY] as? Validation<TP>

/** Adds a validation for this [ModelClass]. */
inline fun <T : Any> ModelClassBuilder<T>.validated(crossinline init: AndValidationBuilder<T>.() -> Unit) {
    val tempValidation = Validation<T> { init() }
    property(VALIDATOR_PROPERTY, tempValidation)
}

/** Adds a validation for this [ModelAttribute]. */
inline fun <T : Any, V> ModelAttributeBuilder<T, V>.validated(crossinline init: AndValidationBuilder<V>.() -> Unit) {
    val tempValidation = Validation<V> { init() }
    property(VALIDATOR_PROPERTY, tempValidation)
}

/** Adds a validation for this [ModelRelation]. */
inline fun <TP> ModelRelationBuilder<*, *, *, TP, *, *>.validated(
    crossinline init: AndValidationBuilder<TP>.() -> Unit
) {
    val tempValidation = Validation<TP> { init() }
    property(VALIDATOR_PROPERTY, tempValidation)
}

/** Adds a validation for this reverse [ModelRelation]. */
inline fun <TP> ModelReverseRelationBuilder<*, *, *, TP, *, *>.validated(
    crossinline init: AndValidationBuilder<TP>.() -> Unit
) {
    val tempValidation = Validation<TP> { init() }
    property(VALIDATOR_PROPERTY, tempValidation)
}