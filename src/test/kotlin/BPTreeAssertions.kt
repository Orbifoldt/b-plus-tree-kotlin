import org.assertj.core.api.ObjectAssert
import org.example.BPTree

object BPTreeAssertions {
    fun ObjectAssert<BPTree>.isValid(): ObjectAssert<BPTree> = this.matches { tree ->
        try {
            tree.validate()
            true
        } catch (e: IllegalStateException) {
            // Throw to preserve the stacktrace (instead of setting custom fail message and returning false)
            throw AssertionError("Expected tree to be valid, but found violation: ${e.message}").apply {
                stackTrace = e.stackTrace
            }
        }
    }
}