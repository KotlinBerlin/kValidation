@file:Suppress("SpellCheckingInspection")

package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.constraints.maxLength
import de.kotlinBerlin.kValidation.constraints.minLength
import de.kotlinBerlin.kValidation.constraints.pattern
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationBuilderTest {

    // Some example constraints for Testing
    private fun ValidationBuilder<String>.containsANumber() =
        pattern(".*\\d.*".toRegex()) hint "must have at least one number"

    @Test
    fun singleValidation() {
        val oneValidation = Validation<Register> {
            Register::password.invoke {
                minLength(1)
            }
        }

        Register(password = "a").let { assertEquals(Valid(it), oneValidation(it)) }
        assertEquals(1, countErrors(oneValidation(Register(password = "")), Register::password))
    }

    @Test
    fun disjunctValidations() {
        val twoDisjunctValidations = Validation<Register> {
            Register::password.invoke {
                minLength(1)
            }
            Register::password.invoke {
                maxLength(10)
            }
        }

        Register(password = "a").let {
            assertEquals(
                Valid(it),
                twoDisjunctValidations(it)
            )
        }
        assertEquals(1, countErrors(twoDisjunctValidations(Register(password = "")), Register::password))
        assertEquals(1, countErrors(twoDisjunctValidations(Register(password = "aaaaaaaaaaa")), Register::password))
    }

    @Test
    fun overlappingValidations() {
        val overlappingValidations = Validation<Register> {
            Register::password.invoke {
                minLength(8)
                containsANumber()
            }
        }

        Register(password = "verysecure1").let {
            assertEquals(
                Valid(it),
                overlappingValidations(it)
            )
        }
        assertEquals(1, countErrors(overlappingValidations(Register(password = "9")), Register::password))
        assertEquals(1, countErrors(overlappingValidations(Register(password = "insecure")), Register::password))
        assertEquals(2, countErrors(overlappingValidations(Register(password = "pass")), Register::password))
    }

    @Test
    fun validatingMultipleFields() {
        val overlappingValidations = Validation<Register> {
            Register::password.invoke {
                minLength(8)
                containsANumber()
            }

            Register::email.invoke {
                pattern(".+@.+".toRegex())
            }
        }

        Register(email = "tester@test.com", password = "verysecure1").let {
            assertEquals(
                Valid(it),
                overlappingValidations(it)
            )
        }
        Register(email = "tester@test.com").let {
            assertEquals(1, countFieldsWithErrors(overlappingValidations(it)))
            assertEquals(2, countErrors(overlappingValidations(it), Register::password))
        }
        assertEquals(1, countErrors(overlappingValidations(Register(password = "verysecure1")), Register::email))
        assertEquals(2, countFieldsWithErrors(overlappingValidations(Register())))
    }

    @Test
    fun validatingNullableTypes() {
        val nullableTypeValidation = Validation<Register> {
            Register::referredBy ifPresent {
                pattern(".+@.+".toRegex())
            }
        }

        Register(referredBy = null).let {
            assertEquals(
                Valid(it),
                nullableTypeValidation(it)
            )
        }
        Register(referredBy = "poweruser@test.com").let {
            assertEquals(
                Valid(it),
                nullableTypeValidation(it)
            )
        }
        assertEquals(1, countErrors(nullableTypeValidation(Register(referredBy = "poweruser@")), Register::referredBy))
    }

    @Test
    fun validatingRequiredTypes() {
        val nullableTypeValidation = Validation<Register> {
            Register::referredBy required {
                pattern(".+@.+".toRegex())
            }
        }

        Register(referredBy = "poweruser@test.com").let {
            assertEquals(
                Valid(it),
                nullableTypeValidation(it)
            )
        }

        assertEquals(1, countErrors(nullableTypeValidation(Register(referredBy = null)), Register::referredBy))
        assertEquals(
            1, countErrors(nullableTypeValidation(Register(referredBy = "poweruser@")), Register::referredBy)
        )
    }

    @Test
    fun validatingNestedTypesDirectly() {
        val nestedTypeValidation = Validation<Register> {
            Register::home ifPresent {
                Address::address.invoke {
                    minLength(1)
                }
            }
        }

        Register(home = Address("Home")).let {
            assertEquals(
                Valid(it),
                nestedTypeValidation(it)
            )
        }
        assertEquals(
            1,
            countErrors(nestedTypeValidation(Register(home = Address(""))), Register::home, Address::address)
        )
    }

    @Test
    fun validateLists() {

        data class Data(val registrations: List<Register> = emptyList())

        val listValidation = Validation<Data> {
            Data::registrations allInIterable {
                Register::email.invoke {
                    minLength(3)
                }
            }
        }

        Data().let { assertEquals(Valid(it), listValidation(it)) }
        assertEquals(
            1,
            countErrors(
                listValidation(Data(registrations = listOf(Register(email = "valid"), Register(email = "a")))),
                Data::registrations,
                1,
                Register::email
            )
        )
        Data(registrations = listOf(Register(email = "a"), Register(email = "ab")))
            .let {
                assertEquals(2, countFieldsWithErrors(listValidation(it)))
                assertEquals(1, countErrors(listValidation(it), Data::registrations, 1, Register::email))
            }
    }

    @Test
    fun validateArrays() {

        data class Data(val registrations: Array<Register> = emptyArray()) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Data) return false

                if (!registrations.contentEquals(other.registrations)) return false

                return true
            }

            override fun hashCode(): Int {
                return registrations.contentHashCode()
            }
        }

        val arrayValidation = Validation<Data> {
            Data::registrations allInArray {
                Register::email.invoke {
                    minLength(3)
                }
            }
        }

        Data().let { assertEquals(Valid(it), arrayValidation(it)) }
        assertEquals(
            1,
            countErrors(
                arrayValidation(
                    Data(
                        registrations = arrayOf(
                            Register(email = "valid"),
                            Register(email = "a")
                        )
                    )
                ), Data::registrations, 1, Register::email
            )
        )
        Data(registrations = arrayOf(Register(email = "a"), Register(email = "ab")))
            .let {
                assertEquals(2, countFieldsWithErrors(arrayValidation(it)))
                assertEquals(1, countErrors(arrayValidation(it), Data::registrations, 1, Register::email))
            }
    }

    @Test
    fun validateHashMaps() {

        data class Data(val registrations: Map<String, Register> = emptyMap())

        val mapValidation = Validation<Data> {
            Data::registrations allInMap {
                value {
                    Register::email.invoke {
                        minLength(2)
                    }
                }
            }
        }

        Data().let { assertEquals(Valid(it), mapValidation(it)) }
        Data(
            registrations = mapOf(
                "user1" to Register(email = "valid"),
                "user2" to Register(email = "a")
            )
        )
            .let {
                val tempResult = mapValidation(it)
                assertEquals(0, countErrors(tempResult, Data::registrations, "user1"))
                assertEquals(1, countErrors(tempResult, Data::registrations, "user2"))
                tempResult.groupedErrors
                assertEquals(
                    "must have at least 2 characters",
                    tempResult.errorsAt(Data::registrations, "user2", MAP_VALUE_SELECTOR, Register::email)
                        .first().message
                )
            }
    }

    @Test
    fun composeValidations() {
        val addressValidation = Validation<Address> {
            Address::address { minLength(1) }
        }

        val validation = Validation<Register> {
            Register::home ifPresent {
                run(addressValidation)
            }
        }

        assertEquals(1, countFieldsWithErrors(validation(Register(home = Address()))))
    }

    @Test
    fun replacePlaceholderInString() {
        val validation = Validation<Register> {
            Register::password{ minLength(8) }
        }
        assertType<Invalid<Register>>(validation(Register(password = ""))) {
            assertTrue(it.errorsAt(Register::password)[0].message.contains("8"))
        }
    }

    private data class Register(
        val password: String = "",
        val email: String = "",
        val referredBy: String? = null,
        val home: Address? = null
    )

    private data class Address(val address: String = "", val country: String = "DE")
}
