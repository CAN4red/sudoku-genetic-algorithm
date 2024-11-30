import java.io.File
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Configuration parameters for the algorithm.
 */
data object Config {
    const val POPULATION_SIZE = 200
    const val POPULATION_STAGNATION_DURATION = 90

    const val MUTATION_PROBABILITY = 0.1

    const val DEBUG_MODE = true
}

data object BlockRanges {
    val ranges = listOf(
        Pair((0..2), (0..2)),
        Pair((0..2), (3..5)),
        Pair((0..2), (6..8)),
        Pair((3..5), (0..2)),
        Pair((3..5), (3..5)),
        Pair((3..5), (6..8)),
        Pair((6..8), (0..2)),
        Pair((6..8), (3..5)),
        Pair((6..8), (6..8)),
    )
}

/**
 * Object for reading the initial Sudoku field from a file or user input depending on Config parameters.
 */
object Input {
    private const val FILE_PATH =
        "C:\\Users\\Alexa\\IdeaProjectsKotlin\\sudoku-genetic-algorithm\\src\\input.txt"

    /** The initial Sudoku field. */
    val initialField = readInitialField()

    /**
     * Reads the initial Sudoku field.
     *
     * @return A Sudoku field represented by integers representing the initial field.
     */
    private fun readInitialField(): List<List<Int>> {
        val input = if (Config.DEBUG_MODE) readInput(FILE_PATH) else readInput()
        return processInput(input)
    }

    /**
     * Reads input data from a file or standard input.
     *
     * @param filePath The path to the file (if null, standard input is used).
     * @return A Sudoku field represented by strings read from the input.
     */
    private fun readInput(filePath: String? = null): List<String> {
        return filePath?.let { path ->
            File(path).readLines()
        }
            ?: (1..9).map { readlnOrNull() ?: "" }
    }

    /**
     * Processes the input data and converts it into a list of lists of integers.
     *
     * @param input A list of strings representing the Sudoku field.
     * @return A Sudoku field represented by integers.
     */
    private fun processInput(input: List<String>): List<List<Int>> {
        return input.map { line ->
            line.split(" ").map { it.toDigit() }
        }
    }

    /**
     * Converts a string to an integer.
     * @return An integer, or 0 if the conversion fails.
     */
    private fun String.toDigit(): Int {
        return this.toIntOrNull() ?: 0
    }
}

/**
 * Class representing a Sudoku field.
 *
 * @param copyingField The field to copy from (if null, a new field is generated).
 */
class SudokuField(
    copyingField: List<List<Int>>? = null
) {
    private val _field: MutableList<MutableList<Int>> = copyingField
        ?.map { it.toMutableList() }
        ?.toMutableList()
        ?: generateField()

    /** Gets the current Sudoku field. */
    val field: List<List<Int>> get() = _field

    private var _fitness = calculateFitness()

    /** Gets the fitness value of the field. */
    val fitness get() = _fitness

    /**
     * Performs mutation on the Sudoku field.
     * Randomly swaps numbers in non-protected cells.
     */
    fun mutate() {
        for (range in BlockRanges.ranges) {
            val mutationEvent = Random.nextInt(0..100)
            if (mutationEvent < Config.MUTATION_PROBABILITY * 100) {
                val freeIndices: MutableList<Pair<Int, Int>> = mutableListOf()
                for (i in range.first) {
                    for (j in range.second) {
                        if (Input.initialField[i][j] == 0) {
                            freeIndices.add(Pair(i, j))
                        }
                    }
                }
                val freeIndex1: Pair<Int, Int> = try {
                    freeIndices.random()
                } catch (e: NoSuchElementException) {
                    Pair(0, 0)
                }
                freeIndices.remove(freeIndex1)
                val freeIndex2 = try {
                    freeIndices.random()
                } catch (e: NoSuchElementException) {
                    freeIndex1
                }
                val temp = _field[freeIndex1.first][freeIndex1.second]
                _field[freeIndex1.first][freeIndex1.second] = _field[freeIndex2.first][freeIndex2.second]
                _field[freeIndex2.first][freeIndex2.second] = temp
            }
        }
        _fitness = calculateFitness()
    }

    /**
     * Generates a new Sudoku field based on the initial field.
     *
     * @return A mutable list of lists of integers representing the generated Sudoku field.
     */
    private fun generateField(): MutableList<MutableList<Int>> {
        val generatedField: MutableList<MutableList<Int>> = Input.initialField
            .map { it.toMutableList() }
            .toMutableList()
        for (range in BlockRanges.ranges) {
            val fixedDigits: List<Int> = range.first
                .flatMap { Input.initialField[it].subList(range.second.first, range.second.last + 1) }
                .filter { it != 0 }

            val allowedDigits = (1..9)
                .filter { digit -> !fixedDigits.contains(digit) }
                .toMutableList()

            for (i in range.first) {
                for (j in range.second) {
                    if (generatedField[i][j] == 0) {
                        generatedField[i][j] = allowedDigits.removeAt(allowedDigits.indices.random())
                    }
                }
            }
        }
        return generatedField
    }

    /**
     * Calculates the fitness of the Sudoku field.
     *
     * The fitness is calculated based on the number of duplicate values in columns and blocks.
     * @return An integer representing the fitness value.
     */
    private fun calculateFitness(): Int {
        return calculateColumnFitness().toDouble().pow(2).toInt() +
                calculateRowFitness().toDouble().pow(2).toInt()
    }

    /**
     * Calculates the fitness based on the columns of the Sudoku field.
     *
     * @return An integer representing the column fitness value.
     */
    private fun calculateColumnFitness(): Int {
        var columnFitness = 0
        for (col in _field[0].indices) {
            val seenCount = (1..9).associateWith { 0 }.toMutableMap()
            for (row in _field.indices) {
                val digit = _field[row][col]
                seenCount[digit] = seenCount[digit]!! + 1
            }
            seenCount.values.forEach {
                columnFitness += (if (it == 0) 0 else (it - 1))
            }
        }
        return columnFitness
    }

    /**
     * Calculates the fitness based on the rows of the Sudoku field.
     *
     * @return An integer representing the block fitness value.
     */
    private fun calculateRowFitness(): Int {
        var rowFitness = 0
        for (row in _field) {
            val seenCount = (1..9).associateWith { 0 }.toMutableMap()
            for (digit in row) {
                seenCount[digit] = seenCount[digit]!! + 1
            }
            seenCount.values.forEach {
                rowFitness += (if (it == 0) 0 else (it - 1))
            }
        }
        return rowFitness
    }

    /**
     * Prints the Sudoku field to the console.
     */
    fun printField() {
        _field.forEach { row -> println(row.joinToString(" ")) }
    }
}

/**
 * Class representing a population of Sudoku fields for the genetic algorithm.
 */
class Population {
    private val _population: MutableList<SudokuField> = generatePopulation()

    /** Gets the current population of Sudoku fields. */
    val population: List<SudokuField> get() = _population

    /**
     * Generates the initial population of Sudoku fields.
     *
     * @return A mutable list of SudokuField objects.
     */
    private fun generatePopulation(): MutableList<SudokuField> {
        return (1..Config.POPULATION_SIZE)
            .map { SudokuField() }
            .toMutableList()
    }

    /**
     * Updates the population by performing crossover and mutation.
     */
    fun updatePopulation() {
        _population.shuffle()
        for (i in 0 until Config.POPULATION_SIZE) {
            val parent1 = _population[i]
            val parent2 = _population[(i + 1) % Config.POPULATION_SIZE]
            _population.add(crossover(parent1, parent2))
        }
        if (_population.minBy { it.fitness }.fitness == 0) {
            return
        }
        for (i in _population.indices) {
            _population[i].mutate()
        }
        _population.sortBy { it.fitness }
        _population.subList(Config.POPULATION_SIZE, _population.size).clear()
    }

    /**
     * Performs crossover between two parent Sudoku fields to create a child field.
     *
     *  @param parent1 The first parent Sudoku field.
     *  @param parent2 The second parent Sudoku field.
     *  @return A new SudokuField object representing the child field.
     */
    private fun crossover(parent1: SudokuField, parent2: SudokuField): SudokuField {
        val childField = parent1.field
            .map { it.toMutableList() }
            .toMutableList()
        for (range in BlockRanges.ranges) {
            val randomComponent = Random.nextInt(0..1)
            if (randomComponent == 0) {
                for (i in range.first) {
                    for (j in range.second) {
                        childField[i][j] = parent2.field[i][j]
                    }
                }
            }
        }
        return SudokuField(childField)
    }
}

/**
 * Executes the genetic algorithm to solve the Sudoku puzzle.
 */
fun geneticAlgorithm() {
    var population = Population()
    var bestFitness = Int.MAX_VALUE
    var fitnessStagnationDuration = 0

    var maxFitness = 0
    val avgFitness = mutableListOf<Int>()

    while (true) {
        val bestSudokuField = population.population.minBy { it.fitness }

        maxFitness = max(bestSudokuField.fitness, maxFitness)
        avgFitness.add(bestSudokuField.fitness)

        if (bestFitness > bestSudokuField.fitness) {
            bestFitness = bestSudokuField.fitness
            fitnessStagnationDuration = 0
        } else {
            fitnessStagnationDuration++
        }
        if (fitnessStagnationDuration >= Config.POPULATION_STAGNATION_DURATION) {
            population = Population()
            fitnessStagnationDuration = 0
            maxFitness = 0
            avgFitness.clear()
        }

        if (Config.DEBUG_MODE) {
            println("Best fitness: ${bestSudokuField.fitness}")
        }

        if (bestSudokuField.fitness == 0) {
            bestSudokuField.printField()
            println(avgFitness.sum().toDouble() / avgFitness.size)
            println(maxFitness)
            break
        }

        population.updatePopulation()
    }
}

fun main() {
    val startingTime = System.currentTimeMillis()
    geneticAlgorithm()
    println((System.currentTimeMillis() - startingTime) / 1000.0)
}
// attempt 1
