import ReadInput.getInitialField
import java.io.File
import kotlin.random.Random
import kotlin.random.nextInt

data object Constants {
    const val POPULATION_SIZE = 100
    const val FIELD_SIZE = 9
    const val MUTATION_CHANCE = 0.1
}

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

data class GameField(
    private val initialField: List<List<Int>>,
    private val fieldCopy: List<List<Int>>? = null,
) {
    // performs deep copy if fieldCopy isn't null
    private val _field: List<List<Int>> = fieldCopy?.map { it.toList() } ?: generateField()
    val field get() = _field
    val fitness = calculateFitness()

    private fun calculateFitness(): Int {
        var fitness = 0
        fitness += calculateColumnFitness()
        fitness += calculateBlockFitness()
        return fitness
    }

    private fun calculateColumnFitness(): Int {
        var columnFitness = 0
        for (col in _field[0].indices) {
            val seenCount = (1..9).associateWith { 0 }.toMutableMap()
            for (row in _field.indices) {
                val digit = _field[row][col]
                seenCount[digit] = seenCount[digit]!! + 1
            }
            seenCount.values.forEach {
                columnFitness -= (if (it == 0) 0 else (it - 1))
            }
        }
        return columnFitness
    }

    private fun calculateBlockFitness(): Int {
        var blockFitness = 0
        for (blockRow in 0..<3) {
            for (blockCol in 0..<3) {

                val seenCount = (1..9).associateWith { 0 }.toMutableMap()
                for (col in (3 * blockCol)..<(3 * (blockCol + 1))) {
                    for (row in (3 * blockRow)..<(3 * (blockRow + 1))) {
                        val digit = _field[row][col]
                        seenCount[digit] = seenCount[digit]!! + 1
                    }
                }
                seenCount.values.forEach {
                    blockFitness -= (if (it == 0) 0 else (it - 1))
                }
            }
        }
        return blockFitness
    }

    private fun generateField(): List<List<Int>> {
        return initialField.map { row -> generateRow(row) }
    }

    fun printField() {
        field.forEach { row -> println(row) }
    }

    companion object {
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

        private fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
            val temp = this[index1]
            this[index1] = this[index2]
            this[index2] = temp
        }
    }
}


data class Population(
    private val initialField: List<List<Int>>,
) {
    private val _population = generatePopulation()
    val population get() = _population

    private fun generatePopulation(): List<GameField> {
        return (1..Constants.POPULATION_SIZE).map { GameField(initialField) }
    }
}


fun crossover(
    initialField: List<List<Int>>,
    parent1: GameField,
    parent2: GameField
): Pair<GameField, GameField> {
    val fieldChild1 = mutableListOf<List<Int>>()
    val fieldChild2 = mutableListOf<List<Int>>()
    for (i in 0..<(Constants.FIELD_SIZE)) {
        val crossoverVariant = Random.nextInt(0..1)
        if (crossoverVariant == 1) {
            fieldChild1.add(parent1.field[i])
            fieldChild2.add(parent2.field[i])
        } else {
            fieldChild1.add(parent2.field[i])
            fieldChild2.add(parent1.field[i])
        }
    }
    return Pair(
        first = GameField(initialField, fieldChild1),
        second = GameField(initialField, fieldChild2)
    )
}


fun mutation(initialField: List<List<Int>>, parent: GameField): GameField {
    val fieldChild = mutableListOf<List<Int>>()
    for (i in 0..<(Constants.FIELD_SIZE)) {
        val mutationEvent = Random.nextInt(0..100)
        if (mutationEvent < Constants.MUTATION_CHANCE * 100) {
            fieldChild.add(GameField.generateRow(initialField[i]))
        } else {
            fieldChild.add(parent.field[i])
        }
    }
    return GameField(initialField, fieldChild)
}


fun main() {
    val initialField = getInitialField()
    val population = Population(initialField)
    population.population.forEach {
        it.printField()
        println(it.fitness)
        println()
    }
}
