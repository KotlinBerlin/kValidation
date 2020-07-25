package de.kotlinBerlin.kValidation

/** A path from the root  object that was validated to a sub object. */
sealed class ValidationPath {
    /** The segments that form the path. */
    abstract val segments: Array<out PathDescriptor<*, *>>

    /** Checks whether or not [other] is a [ValidationPath] and has the same [segments]. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ValidationPath) return false

        if (!segments.contentEquals(other.segments)) return false
        return true
    }

    /** The hashcode of the [segments]. */
    override fun hashCode(): Int {
        return segments.contentHashCode()
    }
}

internal class BasicValidationPath(private val aSegmentList: MutableList<PathDescriptor<*, *>>) : ValidationPath() {
    override val segments: Array<out PathDescriptor<*, *>> get() = aSegmentList.toTypedArray()

    init {
        cleanUp()
    }

    override fun toString(): String = segments.fold("this") { previousResult, nextSegment ->
        combineSegments(nextSegment, previousResult)
    }

    private fun combineSegments(
        nextSegment: PathDescriptor<*, *>,
        previousResult: String
    ): String {
        return when (nextSegment) {
            is PropertyPathDescriptor, is FunctionPathDescriptor -> "$previousResult.${nextSegment.name}"
            ThisPathDescriptor -> previousResult
            is MapPathDescriptor<*, *>, is ArrayPathDescriptor, is IterablePathDescriptor -> "$previousResult${nextSegment.name}"
            is ConditionalPathDescriptor -> combineSegments(nextSegment.descriptor, previousResult)
            is CustomPathDescriptor -> "$previousResult.`${nextSegment.name}`"
        }
    }

    private fun cleanUp() {
        val tempIterator = aSegmentList.listIterator()
        for (tempSegment in tempIterator) {
            if (tempSegment is ThisPathDescriptor) {
                tempIterator.remove()
            } else if (tempSegment is ConditionalPathDescriptor) {
                tempIterator.set(tempSegment.descriptor)
            }
        }
    }
}

internal object EmptyValidationPath : ValidationPath() {
    override val segments: Array<PathDescriptor<*, *>> get() = emptyArray()
    override fun toString(): String = "empty"
}