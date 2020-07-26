package de.kotlinBerlin.kValidation.kModel

import de.kotlinBerlin.kModel.*
import de.kotlinBerlin.kValidation.*
import kotlin.reflect.KProperty1

private val cache = hashMapOf<ModelClass<*>, Pair<Boolean, Validation<*>?>>()

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> ModelClass<T>.getFullValidation(): Validation<T> {
    val (createdFlag, _) = cache.getOrPut(this) { Pair(false, null) }
    if (!createdFlag) {
        cache[this] = Pair(true, null)
        val tempTopLevelValidation = validation
        val tempSuperValidation = superClass?.fullValidation
        val tempValidation = Validation<T> {
            if (tempTopLevelValidation != null) {
                run(tempTopLevelValidation)
            }

            attributes.forEach {
                val tempAttributeValidation = it.validation
                if (tempAttributeValidation != null) {
                    val tempDescriptor: PathDescriptor<T, *> = when (it) {
                        is ImmutablePropertyAttribute -> PropertyPathDescriptor(it.property)
                        is MutablePropertyAttribute -> PropertyPathDescriptor(it.property)
                        is FunctionAttribute -> FunctionPathDescriptor(it.function)
                    }
                    tempDescriptor {
                        run(tempAttributeValidation as Validation<Any?>)
                    }
                }
            }

            relations.forEach {
                when (it) {
                    is OneToManyRelation -> {
                        val tempRelation: OneToManyRelation<T, *, T?, MutableCollection<Any>> =
                            it as OneToManyRelation<T, *, T?, MutableCollection<Any>>
                        val tempSourceField: KProperty1<T, MutableCollection<Any>>? = tempRelation.sourceField

                        if (tempSourceField != null) {
                            val tempFieldValidation = tempRelation.validation
                            val tempClassValidation = tempRelation.targetClass.fullValidation

                            tempSourceField {
                                if (tempFieldValidation != null) {
                                    run(tempFieldValidation)
                                }
                                thisPath allInIterable {
                                    thisPath validateIf {
                                        noRepeat()
                                    } invoke {
                                        run(tempClassValidation as Validation<Any>)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val tempRelation: ModelRelation<T, *, T?, *, *, *> =
                            it as ModelRelation<T, *, T?, *, *, *>
                        val tempSourceField: KProperty1<T, Any?>? = it.sourceField

                        if (tempSourceField != null) {
                            val tempFieldValidation = tempRelation.validation
                            val tempClassValidation = tempRelation.targetClass.fullValidation
                            tempSourceField {
                                if (tempFieldValidation != null) {
                                    run(tempFieldValidation as Validation<Any?>)
                                }
                            }
                            tempSourceField validateIf {
                                tempSourceField required {
                                    noRepeat()
                                }
                            } invoke {
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
        cache[this] = Pair(true, tempValidation)
    }
    return object : Validation<T>() {
        override fun validate(aValue: T, aContext: ValidationContext<*>): Boolean {
            val tempPair: Pair<Boolean, Validation<*>?>? = cache[this@getFullValidation]
            return (tempPair?.second as? Validation<T>)?.validate(aValue, aContext) ?: true
        }
    }
}