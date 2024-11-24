import ReadInput.getInitialField
import java.io.File

object ReadInput {
    private const val FILE_PATH =
        "C:\\Users\\Alexa\\IdeaProjectsKotlin\\sudoku-genetic-algorithm\\src\\input.txt"

    fun getInitialField(): List<List<Int>> {
        val input = readInput(FILE_PATH)
        return processInput(input)
    }

    private fun readInput(filePath: String?): List<String> {
        return filePath?.let { path ->
            File(path).readLines()
        }
            ?: (1..9).map { readlnOrNull() ?: "" }
    }

    private fun processInput(input: List<String>): List<List<Int>> {
        return input.map { line ->
            line.split(" ").map { it.toDigit() }
        }
    }

    private fun String.toDigit(): Int {
        return this.toIntOrNull() ?: 0
    }
}

fun generateRow(initialRow: List<Int>): List<Int> {
    val newRow = (1..9).toList().shuffled().toMutableList()
    for (i in newRow.indices) {
        if (newRow[i] != initialRow[i] && initialRow[i] != 0) {
            val swapIndex = newRow.indexOf(initialRow[i])
            newRow.swap(i, swapIndex)
        }
    }
    return newRow
}

fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
    val temp = this[index1]
    this[index1] = this[index2]
    this[index2] = temp
}

fun generateField(initialField: List<List<Int>>): List<List<Int>> {
    return initialField.map { row -> generateRow(row) }
}


fun main() {
    val initialField = getInitialField()
    val generatedField = generateField(initialField)

    println(initialField)
    println()
    println(generatedField)
}