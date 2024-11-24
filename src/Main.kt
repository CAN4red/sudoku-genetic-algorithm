import java.io.File

object ReadInput {
    private const val FILE_PATH = "C:\\Users\\Alexa\\IdeaProjectsKotlin\\sudoku-genetic-algorithm\\src\\input.txt"

    fun getInitialPuzzle(): List<List<Int>> {
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


fun main() {
    println(ReadInput.getInitialPuzzle())
}