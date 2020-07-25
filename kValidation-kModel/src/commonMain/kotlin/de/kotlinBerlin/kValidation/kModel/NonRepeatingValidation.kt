package de.kotlinBerlin.kValidation.kModel

import de.kotlinBerlin.kValidation.*

private const val NON_REPEATING_CONTEXT_KEY = "nonRepeatingList"

internal fun <T : Any> ValidationBuilder<T>.noRepeat(): Unit = run(NonRepeatingValidation)

private object NonRepeatingValidation : Validation<Any>() {
    @Suppress("UNCHECKED_CAST")
    override fun validate(aValue: Any, aContext: ValidationContext<*>): Boolean {
        val tempNonRepeatingList: MutableSet<Any> =
            aContext.getOrPut(NON_REPEATING_CONTEXT_KEY) { mutableSetOf<Any>() } as? MutableSet<Any>
                ?: throw IllegalStateException("")

        return if (tempNonRepeatingList.add(aValue)) {
            true
        } else {
            aContext.push { AndResult(aValue, it) }
            aContext.addInvalidResult { Error("Can not validate the same object twice.", aValue, it) }
            aContext.popAndAddToParent()
            false
        }
    }
}