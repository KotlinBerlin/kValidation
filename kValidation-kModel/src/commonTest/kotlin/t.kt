import de.kotlinBerlin.kModel.MODEL_MANAGER
import de.kotlinBerlin.kModel.dsl.model
import de.kotlinBerlin.kValidation.*
import de.kotlinBerlin.kValidation.kModel.fullValidation
import de.kotlinBerlin.kValidation.kModel.validated
import kotlin.test.Test

class T {

    @Test
    fun t() {
        model {
            modelClass<A> {
                attribute(A::name) {
                    validated {
                        required {
                            maxLength(64)
                        }

                    }
                }
                attribute(A::age) {
                    validated {
                        required {
                            minimum(1)
                            maximum(100)
                        }
                    }
                }
            }

            modelClass<B> {
                superClass(A::class)
                attribute(B::nameB) {
                    validated {
                        minLength(10)
                    }
                }
                B::c.references(C::bs) {
                    reverse {
                        validated {
                            minItems(1)
                            maxItems(5)
                        }
                    }
                }
            }
        }

        val validation = MODEL_MANAGER.getModelClassFor(B::class)?.fullValidation
        val tempB = B("BName".repeat(3),"AName")
        tempB.c = C()
        tempB.age = 5
        val invoke = validation?.invoke(tempB)
    }

    open class A(val name: String?) {
        var age: Long? = null
    }

    class B(val nameB: String, name: String?) : A(name) {
        lateinit var c: C
    }

    class C {
        var bs: MutableCollection<B> = mutableListOf()
    }
}
