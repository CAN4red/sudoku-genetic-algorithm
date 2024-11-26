import ReadInput.getInitialField
import java.io.File
import kotlin.math.abs
import kotlin.random.Random
import kotlin.random.nextInt

data object Constants {
    const val FIELD_SIZE = 9
    const val POPULATION_SIZE = 1000
    const val MUTATION_PROBABILITY = 0.1
    const val CROSSOVER_PROBABILITY = 0.95
}

object ReadInput {
    private const val FILE_PATH =
        "C:\\Users\\Alexa\\IdeaProjectsKotlin\\sudoku-genetic-algorithm\\src\\input.txt"

    fun getInitialField(): List<List<Int>> {
        val input = readInput(FILE_PATH)
//        val input = readInput()
        return processInput(input)
    }

    private fun readInput(filePath: String? = null): List<String> {
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

private fun <T> MutableList<T>.swapInPlace(index1: Int, index2: Int) {
    val temp = this[index1]
    this[index1] = this[index2]
    this[index2] = temp
}

private fun <T> MutableList<T>.swapWithCopy(index1: Int, index2: Int): MutableList<T> {
    val newList = this.toMutableList()
    val temp = newList[index1]
    newList[index1] = newList[index2]
    newList[index2] = temp
    return newList
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
                columnFitness += (if (it == 0) 0 else (it - 1))
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
                    blockFitness += (if (it == 0) 0 else (it - 1))
                }
            }
        }
        return blockFitness
    }

    private fun generateField(): List<List<Int>> {
        return initialField.map { row -> generateRow(row) }
    }

    fun printField() {
        field.forEach { row -> println(row.joinToString(" ")) }
    }

    companion object {
        fun generateRow(initialRow: List<Int>): List<Int> {
            val newRow = (1..9).toList().shuffled().toMutableList()
            for (i in newRow.indices) {
                if (newRow[i] != initialRow[i] && initialRow[i] != 0) {
                    val swapIndex = newRow.indexOf(initialRow[i])
                    newRow.swapInPlace(i, swapIndex)
                }
            }
            return newRow
        }
    }
}


data class Population(
    private val initialField: List<List<Int>>,
    private val populationCopy: List<GameField>? = null,
) {
    private val _population = populationCopy ?: generatePopulation()
    val population get() = _population

    private fun generatePopulation(): List<GameField> {
        return (1..Constants.POPULATION_SIZE).map { GameField(initialField) }
    }

    fun getWeightedPool(): List<GameField> {
        val sortedPopulation = _population.sortedByDescending { it.fitness }
        val weights = (1.._population.size).toList()
        val randomPool = mutableListOf<GameField>()
        repeat(Constants.POPULATION_SIZE) {
            randomPool.add(weightedRandomChoice(sortedPopulation, weights))
        }
        return randomPool
    }

    fun getRandomPool(): List<GameField> {
        val weights = _population.map { abs(it.fitness - _population[0].fitness) }
        val randomPool = mutableListOf<GameField>()
        repeat(Constants.POPULATION_SIZE) {
            randomPool.add(weightedRandomChoice(_population, weights))
        }
        return randomPool
    }

    private fun <T> weightedRandomChoice(items: List<T>, weights: List<Int>): T {
        val totalWeight = weights.sum()
        val randomVariable = Random.nextInt(0, totalWeight)
        var cumulativeWeight = 0
        for (i in items.indices) {
            cumulativeWeight += weights[i]
            if (randomVariable < cumulativeWeight) {
                return items[i]
            }
        }
        return items.last()
    }

    fun getChildren(): List<GameField> {
        val childrenPool = mutableListOf<GameField>()
        for (i in 0..<(Constants.POPULATION_SIZE / 2)) {
            var gameField1 = _population[i]
            var gameField2 = _population[(i + 1) % _population.size]
            val crossoverChance = Random.nextInt(0..100)
            if (crossoverChance < Constants.CROSSOVER_PROBABILITY * 100) {
                val childrenPair = crossover(initialField, gameField1, gameField2)
                gameField1 = childrenPair.first
                gameField2 = childrenPair.second
            }
            childrenPool.add(rearrangementMutation(initialField, gameField1))
            childrenPool.add(rearrangementMutation(initialField, gameField2))
        }
        return childrenPool
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


fun refillingMutation(initialField: List<List<Int>>, parent: GameField): GameField {
    val fieldChild = mutableListOf<List<Int>>()
    for (i in 0..<(Constants.FIELD_SIZE)) {
        val mutationEvent = Random.nextInt(0..100)
        if (mutationEvent < Constants.MUTATION_PROBABILITY * 100) {
            fieldChild.add(GameField.generateRow(initialField[i]))
        } else {
            fieldChild.add(parent.field[i])
        }
    }
    return GameField(initialField, fieldChild)
}


fun rearrangementMutation(initialField: List<List<Int>>, parent: GameField): GameField {
    val fieldChild = mutableListOf<List<Int>>()
    for (i in 0..<(Constants.FIELD_SIZE)) {
        val mutationEvent = Random.nextInt(0..100)
        if (mutationEvent < Constants.MUTATION_PROBABILITY * 100) {
            val freeIndices = initialField[i]
                .mapIndexedNotNull { index, value -> if (value == 0) index else null }
                .toMutableList()
            val freeIndex1: Int = try {
                freeIndices.random()
            } catch (e: NoSuchElementException) {
                0
            }
            freeIndices.remove(freeIndex1)
            val freeIndex2 = try {
                freeIndices.random()
            } catch (e: NoSuchElementException) {
                freeIndex1
            }
            fieldChild.add(parent.field[i].toMutableList().swapWithCopy(freeIndex1, freeIndex2))
        } else {
            fieldChild.add(parent.field[i])
        }
    }
    return GameField(initialField, fieldChild)
}


fun geneticAlgorithm() {
    val initialField = getInitialField()
    var population = Population(initialField)
    while (true) {
        val matingPool = population.getWeightedPool()
        matingPool.shuffled()
        val matingPopulation = Population(initialField, matingPool)
        val childrenPool = matingPopulation.getChildren()
        val bestGameField = childrenPool.minByOrNull { it.fitness }
        println("Best fitness: ${bestGameField?.fitness}")
        if (bestGameField?.fitness == 0) {
            bestGameField.printField()
            break
        }
        population = Population(initialField, childrenPool)
    }
}


fun main() {
    geneticAlgorithm()
}
