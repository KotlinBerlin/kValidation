@file:Suppress("UNCHECKED_CAST", "unused")

package de.kotlinBerlin.kValidation.kModel

import de.kotlinBerlin.kModel.*
import de.kotlinBerlin.kModel.dsl.ModelAttributeBuilder
import de.kotlinBerlin.kModel.dsl.ModelClassBuilder
import de.kotlinBerlin.kModel.dsl.ModelRelationBuilder
import de.kotlinBerlin.kModel.dsl.ModelReverseRelationBuilder
import de.kotlinBerlin.kValidation.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

const val VALIDATOR_PROPERTY = "kValidator"

inline val <T : Any> ModelClass<T>.validation: Validation<T>?
    get() = this.modelProperties[VALIDATOR_PROPERTY] as? Validation<T>

val <T : Any> ModelClass<T>.fullValidation: Validation<T> by object : ReadOnlyProperty<ModelClass<T>, Validation<T>> {

    private val cache = hashMapOf<ModelClass<*>, Pair<Boolean, Validation<*>?>>()

    override fun getValue(thisRef: ModelClass<T>, property: KProperty<*>): Validation<T> {
        val (createdFlag, _) = cache.getOrPut(thisRef) { Pair(false, null) }
        if (!createdFlag) {
            cache[thisRef] = Pair(true, null)
            val tempTopLevelValidation = thisRef.validation
            val tempSuperValidation = thisRef.superClass?.fullValidation
            val tempValidation = Validation<T> {
                if (tempTopLevelValidation != null) {
                    run(tempTopLevelValidation)
                }

                thisRef.attributes.forEach {
                    val tempAttributeValidation = it.validation
                    if (tempAttributeValidation != null) {
                        val tempDescriptor: PathDescriptor<T, *> = when (it) {
                            is ImmutablePropertyAttribute -> PropertyPathDescriptor(it.property)
                            is MutablePropertyAttribute -> PropertyPathDescriptor(it.property)
                            is FunctionAttribute -> FunctionPathDescriptor(it.function)
                        }
                        tempDescriptor validate {
                            run(tempAttributeValidation as Validation<Any?>)
                        }
                    }
                }

                thisRef.relations.forEach {
                    when (it) {
                        is OneToManyRelation -> {
                            val tempRelation: OneToManyRelation<T, *, T?, MutableCollection<Any>> =
                                it as OneToManyRelation<T, *, T?, MutableCollection<Any>>
                            val tempSourceField = tempRelation.sourceField

                            if (tempSourceField != null) {
                                val tempFieldValidation = tempRelation.validation
                                val tempClassValidation = tempRelation.targetClass.fullValidation

                                tempSourceField validate {
                                    if (tempFieldValidation != null) {
                                        run(tempFieldValidation)
                                    }
                                    onEach {
                                        thisPath validateIf {
                                            noRepeat()
                                        } validate {
                                            run(tempClassValidation as Validation<Any>)
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            val tempRelation: ModelRelation<T, *, T?, *, *, *> =
                                it as ModelRelation<T, *, T?, *, *, *>
                            val tempSourceField = it.sourceField

                            if (tempSourceField != null) {
                                val tempFieldValidation = tempRelation.validation
                                val tempClassValidation = tempRelation.targetClass.fullValidation
                                tempSourceField validate {
                                    if (tempFieldValidation != null) {
                                        run(tempFieldValidation as Validation<Any?>)
                                    }
                                }
                                tempSourceField validateIf {
                                    tempSourceField required {
                                        noRepeat()
                                    }
                                } validate {
                                    run(tempClassValidation as Validation<Any?>)
                                }
                            }
                        }
                    }
                }

                if (tempSuperValidation != null) {
                    run(tempSuperValidation)
                }
            }
            cache[thisRef] = Pair(true, tempValidation)
        }
        return object : Validation<T> {
            override fun validate(aValue: T, aContext: ValidationContext): ValidationResult<T> {
                val tempContext =
                    if (aContext is WrappingValidationContext && aContext.wrapped is NonRepeatingContext) aContext else WrappingValidationContext(
                        aContext,
                        NonRepeatingContext()
                    )
                val tempPair = cache[thisRef]
                return (tempPair?.second as? Validation<T>)?.validate(aValue, tempContext) ?: Valid(aValue)
            }
        }
    }
}

inline val <T : Any, V> ModelAttribute<T, V>.validation: Validation<V>?
    get() = this.modelProperties[VALIDATOR_PROPERTY] as? Validation<V>

inline val <TP> ModelRelation<*, *, *, TP, *, *>.validation: Validation<TP>?
    get() = this.modelProperties[VALIDATOR_PROPERTY] as? Validation<TP>

inline fun <T : Any> ModelClassBuilder<T>.validated(crossinline init: AndValidationBuilder<T>.() -> Unit) {
    val tempValidation = Validation<T> { init() }
    property(VALIDATOR_PROPERTY, tempValidation)
}

inline fun <T : Any, V> ModelAttributeBuilder<T, V>.validated(crossinline init: AndValidationBuilder<V>.() -> Unit) {
    val tempValidation = Validation<V> { init() }
    property(VALIDATOR_PROPERTY, tempValidation)
}

inline fun <TP> ModelRelationBuilder<*, *, *, TP, *, *>.validated(
    crossinline init: AndValidationBuilder<TP>.() -> Unit
) {
    val tempValidation = Validation<TP> { init() }
    property(VALIDATOR_PROPERTY, tempValidation)
}

inline fun <TP> ModelReverseRelationBuilder<*, *, *, TP, *, *>.validated(
    crossinline init: AndValidationBuilder<TP>.() -> Unit
) {
    val tempValidation = Validation<TP> { init() }
    property(VALIDATOR_PROPERTY, tempValidation)
}