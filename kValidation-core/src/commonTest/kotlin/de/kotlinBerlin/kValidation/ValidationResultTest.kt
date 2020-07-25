package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.constraints.maxLength
import de.kotlinBerlin.kValidation.constraints.minLength
import de.kotlinBerlin.kValidation.constraints.pattern
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidationResultTest {

    @Test
    fun singleValidation() {
        val validation = Validation<Person> {
            Person::name.invoke {
                minLength(1)
            }

            Person::addresses allInIterable {
                Address::city.invoke {
                    City::postalCode.invoke {
                        minLength(4)
                        maxLength(5)
                        pattern("\\d{4,5}") hint ("must be a four or five digit number")
                    }
                }
            }
        }

        val result = validation(Person("", addresses = listOf(Address(City("", "")))))
        assertType<Invalid<Person>>(result) {
            assertEquals(3, it.flatErrors.size)
            val (firstError, secondError, thirdError) = it.flatErrors

            assertEquals("this.name", firstError.dataPath.toString())
            assertEquals("must have at least 1 characters", firstError.message)

            assertEquals("this.addresses[0].city.postalCode", secondError.dataPath.toString())
            assertEquals("must have at least 4 characters", secondError.message)

            assertEquals("this.addresses[0].city.postalCode", thirdError.dataPath.toString())
            assertEquals("must be a four or five digit number", thirdError.message)
        }
    }

    private data class Person(val name: String, val addresses: List<Address>)
    private data class Address(val city: City)
    private data class City(val postalCode: String, val cityName: String)
}

