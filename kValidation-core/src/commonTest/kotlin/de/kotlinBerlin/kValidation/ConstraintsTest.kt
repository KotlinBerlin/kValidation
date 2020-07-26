@file:Suppress("SpellCheckingInspection")

package de.kotlinBerlin.kValidation

import de.kotlinBerlin.kValidation.ConstraintsTest.TCPPacket.*
import de.kotlinBerlin.kValidation.constraints.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ConstraintsTest {

    @Test
    fun typeConstraint() {
        val anyValidation = Validation<Any> { type<String>() }
        assertEquals(
            Valid("This is a string"),
            anyValidation("This is a string")
        )

        assertEquals(1, countFieldsWithErrors(anyValidation(1)))
        assertEquals(1, countFieldsWithErrors(anyValidation(1.0)))
        assertEquals(1, countFieldsWithErrors(anyValidation(true)))

        val anyNumberValidation = Validation<Any> { type<Int>() }
        assertEquals(Valid(1), anyNumberValidation(1))
        assertEquals(1, countFieldsWithErrors(anyNumberValidation("String")))
        assertEquals(1, countFieldsWithErrors(anyNumberValidation(true)))

        assertEquals("must be of the correct type", anyValidation(1).errorsAt(includeSubErrors = true)[0].message)
        assertEquals(
            "must be of the correct type",
            anyNumberValidation("String").errorsAt(includeSubErrors = true)[0].message
        )
    }

    @Test
    fun nullableTypeConstraint() {
        val anyValidation = Validation<Any?> { type<String?>() }
        assertEquals(
            Valid("This is a string"),
            anyValidation("This is a string")
        )
        assertEquals(
            Valid(null),
            anyValidation(null)
        )
    }

    @Test
    fun stringEnumConstraint() {
        val validation = Validation<String> { oneOf("OK", "CANCEL") }
        assertEquals(Valid("OK"), validation("OK"))
        assertEquals(Valid("CANCEL"), validation("CANCEL"))
        assertEquals(1, countFieldsWithErrors(validation("???")))
        assertEquals(1, countFieldsWithErrors(validation("")))

        assertEquals("must be one of: 'OK', 'CANCEL'", validation("").errorsAt(includeSubErrors = true)[0].message)
    }


    enum class TCPPacket {
        SYN, ACK, SYNACK
    }

    @Test
    fun kotlinEnumConstraint() {
        val partialEnumValidation = Validation<TCPPacket> { oneOf(SYN, ACK) }
        assertEquals(Valid(SYN), partialEnumValidation(SYN))
        assertEquals(Valid(ACK), partialEnumValidation(ACK))
        assertEquals(1, countFieldsWithErrors(partialEnumValidation(SYNACK)))

        assertEquals(
            "must be one of: 'SYN', 'ACK'",
            partialEnumValidation(SYNACK).errorsAt(includeSubErrors = true)[0].message
        )
    }

    @Test
    fun constConstraint() {
        val validation = Validation<String> { const("Konform") }
        assertEquals(Valid("Konform"), validation("Konform"))
        assertEquals(1, countFieldsWithErrors(validation("")))


        val nullableConstNullValidation = Validation<String?> { const(null) }
        assertEquals(Valid(null), nullableConstNullValidation(null))
        assertEquals(1, countFieldsWithErrors(nullableConstNullValidation("")))
        assertEquals(1, countFieldsWithErrors(nullableConstNullValidation("Konform")))

        val nullableConstValidation = Validation<String?> { const("Konform") }
        assertEquals(Valid("Konform"), nullableConstValidation("Konform"))
        assertEquals(1, countFieldsWithErrors(nullableConstValidation(null)))
        assertEquals(1, countFieldsWithErrors(nullableConstValidation("Konverse")))

        assertEquals("must be 'Konform'", validation("Konverse").errorsAt(includeSubErrors = true)[0].message)
        assertEquals(
            "must be null",
            nullableConstNullValidation("Konform").errorsAt(includeSubErrors = true)[0].message
        )
        assertEquals("must be 'Konform'", nullableConstValidation(null).errorsAt(includeSubErrors = true)[0].message)
    }

    @Test
    fun maximumConstraint() {
        val validation = Validation<Number> { max(10) }

        assertEquals(Valid(Double.NEGATIVE_INFINITY), validation(Double.NEGATIVE_INFINITY))
        assertEquals(Valid(-10), validation(-10))
        assertEquals(Valid(9), validation(9))
        assertEquals(Valid(10), validation(10))
        assertEquals(Valid(10.0), validation(10.0))

        assertEquals(1, countFieldsWithErrors(validation(10.00001)))
        assertEquals(1, countFieldsWithErrors(validation(11)))
        assertEquals(1, countFieldsWithErrors(validation(Double.POSITIVE_INFINITY)))

        assertEquals(
            Valid(Double.POSITIVE_INFINITY),
            Validation<Number> { max(Double.POSITIVE_INFINITY) }(Double.POSITIVE_INFINITY)
        )

        assertEquals("must be at most '10'", validation(11).errorsAt(includeSubErrors = true)[0].message)
    }

    @Test
    fun exclusiveMaximumConstraint() {
        val validation = Validation<Number> { max(10, exclusive = true) }

        assertEquals(Valid(Double.NEGATIVE_INFINITY), validation(Double.NEGATIVE_INFINITY))
        assertEquals(Valid(-10), validation(-10))
        assertEquals(Valid(9), validation(9))
        assertEquals(Valid(9.99999999), validation(9.99999999))

        assertEquals(1, countFieldsWithErrors(validation(10)))
        assertEquals(1, countFieldsWithErrors(validation(10.0)))
        assertEquals(1, countFieldsWithErrors(validation(10.00001)))
        assertEquals(1, countFieldsWithErrors(validation(11)))
        assertEquals(1, countFieldsWithErrors(validation(Double.POSITIVE_INFINITY)))
        assertEquals(
            1,
            countFieldsWithErrors(Validation<Number> {
                max(
                    Double.POSITIVE_INFINITY,
                    exclusive = true
                )
            }(Double.POSITIVE_INFINITY))
        )


        assertEquals("must be less than '10'", validation(11).errorsAt(includeSubErrors = true)[0].message)
    }

    @Test
    fun minimumConstraint() {
        val validation = Validation<Number> { min(10) }

        assertEquals(Valid(Double.POSITIVE_INFINITY), validation(Double.POSITIVE_INFINITY))
        assertEquals(Valid(20), validation(20))
        assertEquals(Valid(11), validation(11))
        assertEquals(Valid(10.1), validation(10.1))
        assertEquals(Valid(10.0), validation(10.0))

        assertEquals(1, countFieldsWithErrors(validation(9.99999999999)))
        assertEquals(1, countFieldsWithErrors(validation(8)))
        assertEquals(1, countFieldsWithErrors(validation(Double.NEGATIVE_INFINITY)))

        assertEquals(
            Valid(Double.NEGATIVE_INFINITY),
            Validation<Number> { min(Double.NEGATIVE_INFINITY) }(Double.NEGATIVE_INFINITY)
        )

        assertEquals("must be at least '10'", validation(9).errorsAt(includeSubErrors = true)[0].message)
    }

    @Test
    fun minimumExclusiveConstraint() {
        val validation = Validation<Number> { min(10, exclusive = true) }

        assertEquals(Valid(Double.POSITIVE_INFINITY), validation(Double.POSITIVE_INFINITY))
        assertEquals(Valid(20), validation(20))
        assertEquals(Valid(11), validation(11))
        assertEquals(Valid(10.1), validation(10.1))

        assertEquals(1, countFieldsWithErrors(validation(10)))
        assertEquals(1, countFieldsWithErrors(validation(10.0)))
        assertEquals(1, countFieldsWithErrors(validation(9.99999999999)))
        assertEquals(1, countFieldsWithErrors(validation(8)))
        assertEquals(1, countFieldsWithErrors(validation(Double.NEGATIVE_INFINITY)))
        assertEquals(
            1,
            countFieldsWithErrors(Validation<Number> {
                min(
                    Double.NEGATIVE_INFINITY,
                    exclusive = true
                )
            }(Double.NEGATIVE_INFINITY))
        )


        assertEquals("must be greater than '10'", validation(9).errorsAt(includeSubErrors = true)[0].message)
    }

    @Test
    fun minLengthConstraint() {
        val validation = Validation<String> { minLength(10) }

        assertEquals(Valid("HelloWorld"), validation("HelloWorld"))
        assertEquals(Valid("Hello World"), validation("Hello World"))

        assertEquals(1, countFieldsWithErrors(validation("Hello")))
        assertEquals(1, countFieldsWithErrors(validation("")))

        assertEquals("must have at least 10 characters", validation("").errorsAt(includeSubErrors = true)[0].message)
    }

    @Test
    fun maxLengthConstraint() {
        val validation = Validation<String> { maxLength(10) }

        assertEquals(Valid("HelloWorld"), validation("HelloWorld"))
        assertEquals(Valid("Hello"), validation("Hello"))
        assertEquals(Valid(""), validation(""))

        assertEquals(1, countFieldsWithErrors(validation("Hello World")))

        assertEquals(
            "must have at most 10 characters",
            validation("Hello World").errorsAt(includeSubErrors = true)[0].message
        )
    }

    @Test
    fun patternConstraint() {
        val validation = Validation<String> { pattern(".+@.+") }

        assertEquals(Valid("a@a"), validation("a@a"))
        assertEquals(Valid("a@a@a@a"), validation("a@a@a@a"))
        assertEquals(Valid(" a@a "), validation(" a@a "))

        assertEquals(1, countFieldsWithErrors(validation("a")))
        assertEquals("must match the expected pattern", validation("").errorsAt(includeSubErrors = true)[0].message)

        val compiledRegexValidation = Validation<String> {
            pattern("^\\w+@\\w+\\.\\w+$".toRegex())
        }

        assertEquals(
            Valid("tester@example.com"),
            compiledRegexValidation("tester@example.com")
        )
        assertEquals(1, countFieldsWithErrors(compiledRegexValidation("tester@example")))
        assertEquals(1, countFieldsWithErrors(compiledRegexValidation(" tester@example.com")))
        assertEquals(1, countFieldsWithErrors(compiledRegexValidation("tester@example.com ")))

        assertEquals(
            "must match the expected pattern",
            compiledRegexValidation("").errorsAt(includeSubErrors = true)[0].message
        )
    }

    @Test
    fun minSizeConstraint() {
        val validation = Validation<List<String>> {
            minItems(1)
        }

        assertEquals(Valid(listOf("a", "b")), validation(listOf("a", "b")))
        assertEquals(Valid(listOf("a")), validation(listOf("a")))

        assertEquals(1, countFieldsWithErrors(validation(emptyList())))


        val arrayValidation = Validation<Array<String>> {
            minItems(1)
        }

        arrayOf("a", "b").let { assertEquals(Valid(it), arrayValidation(it)) }
        arrayOf("a").let { assertEquals(Valid(it), arrayValidation(it)) }

        assertEquals(1, countFieldsWithErrors(arrayValidation(emptyArray())))

        val mapValidation = Validation<Map<String, Int>> { minItems(1) }

        assertEquals(
            Valid(mapOf("a" to 0, "b" to 1)),
            mapValidation(mapOf("a" to 0, "b" to 1))
        )
        assertEquals(Valid(mapOf("a" to 0)), mapValidation(mapOf("a" to 0)))

        assertEquals(1, countFieldsWithErrors(mapValidation(emptyMap())))

        assertEquals("must have at least 1 item", validation(emptyList()).errorsAt(includeSubErrors = true)[0].message)
    }

    @Test
    fun maxSizeConstraint() {
        val validation = Validation<List<String>> { maxItems(1) }

        assertEquals(Valid(emptyList()), validation(emptyList()))
        assertEquals(Valid(listOf("a")), validation(listOf("a")))

        assertEquals(1, countFieldsWithErrors(validation(listOf("a", "b"))))

        val arrayValidation = Validation<Array<String>> { maxItems(1) }
        emptyArray<String>().let { assertEquals(Valid(it), arrayValidation(it)) }
        arrayOf("a").let { assertEquals(Valid(it), arrayValidation(it)) }

        assertEquals(1, countFieldsWithErrors(arrayValidation(arrayOf("a", "b"))))

        val mapValidation = Validation<Map<String, Int>> { maxItems(1) }

        assertEquals(Valid(emptyMap()), mapValidation(emptyMap()))
        assertEquals(Valid(mapOf("a" to 0)), mapValidation(mapOf("a" to 0)))

        assertEquals(1, countFieldsWithErrors(mapValidation(mapOf("a" to 0, "b" to 1))))

        assertEquals(
            "must have at most 1 item",
            mapValidation(mapOf("a" to 0, "b" to 1)).errorsAt(includeSubErrors = true)[0].message
        )
    }

    @Test
    fun minPropertiesConstraint() {
        val validation = Validation<Map<String, Int>> { minItems(1) }

        assertEquals(
            Valid(mapOf("a" to 0, "b" to 1)),
            validation(mapOf("a" to 0, "b" to 1))
        )
        assertEquals(Valid(mapOf("a" to 0)), validation(mapOf("a" to 0)))

        assertEquals(1, countFieldsWithErrors(validation(emptyMap())))

        assertEquals("must have at least 1 item", validation(emptyMap()).errorsAt(includeSubErrors = true)[0].message)
    }

    @Test
    fun maxPropertiesConstraint() {
        val validation = Validation<Map<String, Int>> { maxItems(1) }

        assertEquals(Valid(emptyMap()), validation(emptyMap()))
        assertEquals(Valid(mapOf("a" to 0)), validation(mapOf("a" to 0)))

        assertEquals(1, countFieldsWithErrors(validation(mapOf("a" to 0, "b" to 1))))

        assertEquals(
            "must have at most 1 item",
            validation(mapOf("a" to 0, "b" to 1)).errorsAt(includeSubErrors = true)[0].message
        )
    }

    @Test
    fun uniqueItemsConstraint() {
        val validation = Validation<List<String>> { distinct() }

        assertEquals(Valid(emptyList()), validation(emptyList()))
        assertEquals(Valid(listOf("a")), validation(listOf("a")))
        assertEquals(Valid(listOf("a", "b")), validation(listOf("a", "b")))

        val mapValidation = Validation<Map<String, String>> { distinctValues() }

        assertEquals(Valid(emptyMap()), mapValidation(emptyMap()))
        assertEquals(
            Valid(mapOf("a" to "b")),
            mapValidation(mapOf("a" to "b"))
        )
        assertEquals(
            Valid(mapOf("a" to "b", "b" to "c")),
            mapValidation(mapOf("a" to "b", "b" to "c"))
        )

        assertEquals(1, countFieldsWithErrors(validation(listOf("a", "a"))))

        val arrayValidation = Validation<Array<String>> { distinct() }

        emptyArray<String>().let { assertEquals(Valid(it), arrayValidation(it)) }
        arrayOf("a").let { assertEquals(Valid(it), arrayValidation(it)) }
        arrayOf("a", "b").let { assertEquals(Valid(it), arrayValidation(it)) }

        assertEquals(1, countFieldsWithErrors(mapValidation(mapOf("a" to "b", "b" to "b"))))
        assertEquals(1, countFieldsWithErrors(arrayValidation(arrayOf("a", "a"))))
        assertEquals(
            "all items must be unique",
            validation(listOf("a", "a")).errorsAt(includeSubErrors = true)[0].message
        )
    }
}
