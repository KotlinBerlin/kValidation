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
            Person::name.validate {
                minLength(1)
            }

            Person::addresses onEachIterable  {
                Address::city.validate {
                    City::postalCode.validate {
                        minLength(4)
                        maxLength(5)
                        pattern("\\d{4,5}") hint ("must be a four or five digit number")
                    }
                }
            }
        }

        val result = validation(Person("", addresses = listOf(Address(City("", "")))))
        assertEquals(3, result.errors.size)
        val (firstError, secondError, thirdError) = result.errors

        assertEquals("this.name", firstError.dataPath)
        assertEquals("'' must have at least 1 characters", firstError.message)

        assertEquals("this.addresses[0].city.postalCode", secondError.dataPath)
        assertEquals("'' must have at least 4 characters", secondError.message)

        assertEquals("this.addresses[0].city.postalCode", thirdError.dataPath)
        assertEquals("must be a four or five digit number", thirdError.message)
    }

    private data class Person(val name: String, val addresses: List<Address>)
    private data class Address(val city: City)
    private data class City(val postalCode: String, val cityName: String)
}

