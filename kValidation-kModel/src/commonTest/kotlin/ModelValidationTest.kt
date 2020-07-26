@file:Suppress("UNUSED_VARIABLE")

import de.kotlinBerlin.kModel.MODEL_MANAGER
import de.kotlinBerlin.kModel.dsl.model
import de.kotlinBerlin.kValidation.Invalid
import de.kotlinBerlin.kValidation.Valid
import de.kotlinBerlin.kValidation.constraints.*
import de.kotlinBerlin.kValidation.kModel.fullValidation
import de.kotlinBerlin.kValidation.kModel.validated
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ModelValidationTest {

    @Test
    fun validateModel() {
        model {
            modelClass<A> {
                attribute(A::name) {
                    validated {
                        thisPath required {
                            maxLength(64)
                        }
                    }
                }
                attribute(A::age) {
                    validated {
                        thisPath required {
                            min(1)
                            max(100)
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

        val tempValidation = MODEL_MANAGER.getModelClassFor(B::class)!!.fullValidation
        val tempValidB = B("BName".repeat(3), "AName")
        val tempInvalidB = B("BName".repeat(2), "AName".repeat(100))
        tempValidB.c = C()
        tempValidB.age = 5
        tempInvalidB.age = -1
        tempValidB.c?.bs?.add(tempValidB)
        //tempB.c?.bs?.add(tempB2)
        val tempResult1 = tempValidation.invoke(tempValidB)
        assertType<Valid<B>>(tempResult1) {
            assertSame(tempValidB, it.value)
        }

        val tempResult2 = tempValidation.invoke(tempInvalidB)
        assertType<Invalid<B>>(tempResult2) {
            assertSame(tempInvalidB, it.value)
            assertEquals(2, it.flatErrors.size)
            assertEquals("must be at least '1'", it.errorsAt(B::age).first().message)
            assertEquals("must have at most 64 characters", it.errorsAt(B::name).first().message)
        }

        tempValidB.c?.bs?.add(tempInvalidB)

        val tempResult3 = tempValidation.invoke(tempValidB)
        assertType<Invalid<B>>(tempResult3) {
            assertSame(tempValidB, it.value)
            assertEquals(2, it.flatErrors.size)
            assertEquals("must be at least '1'", it.errorsAt(B::c, C::bs, 1, A::age).first().message)
            assertEquals("must have at most 64 characters", it.errorsAt(B::c, C::bs, 1, A::name).first().message)
        }
    }

    open class A(val name: String?) {
        var age: Long? = null
    }

    class B(val nameB: String, name: String?) : A(name) {
        var c: C? = null
    }

    class C {
        var bs: MutableCollection<B> = mutableListOf()
    }

    private inline fun <reified T> assertType(anObject: Any?, typedCheck: (T) -> Unit) {
        assertTrue("object should be of type: " + T::class.simpleName + "but was: " + (if (anObject == null) "null" else anObject::class.simpleName)) { anObject is T }
        typedCheck.invoke(anObject as T)
    }
}
