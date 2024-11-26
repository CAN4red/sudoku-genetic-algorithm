import ReadInput.getInitialField
import java.io.File
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random
import kotlin.random.nextInt

data object Constants {
    const val FIELD_SIZE = 9
    const val POPULATION_SIZE = 300
    const val POPULATION_COUNT = 1000
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
        fitness += calculateColumnFitness().toDouble().pow(2.0).toInt()
        fitness += calculateBlockFitness().toDouble().pow(2.0).toInt()
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
        val sortedPopulation = _population.sortedByDescending { it.fitness }.toMutableList()
        val weights = (1.._population.size).toMutableList()
        val randomPool = mutableListOf<GameField>()
        repeat(Constants.POPULATION_SIZE) {
            val currentChoiceId = weightedRandomChoice(sortedPopulation, weights)
            val currentChoice = sortedPopulation.removeAt(currentChoiceId)
            weights.removeAt(currentChoiceId)
            randomPool.add(currentChoice)
        }
        return randomPool
    }

//    fun getRandomPool(): List<GameField> {
//        val weights = _population.map { abs(it.fitness - _population[0].fitness) }
//        val randomPool = mutableListOf<GameField>()
//        repeat(Constants.POPULATION_SIZE) {
//            randomPool.add(weightedRandomChoice(_population, weights))
//        }
//        return randomPool
//    }

    private fun <T> weightedRandomChoice(items: List<T>, weights: List<Int>): Int {
        val totalWeight = weights.sum()
        val randomVariable = Random.nextInt(0, totalWeight)
        var cumulativeWeight = 0
        for (i in items.indices) {
            cumulativeWeight += weights[i]
            if (randomVariable < cumulativeWeight) {
                return i
            }
        }
        return weights.last() - 1
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
            gameField1 = rearrangementMutation(initialField, gameField1)
            gameField2 = rearrangementMutation(initialField, gameField2)
            if (gameField1.fitness <= 16) {
                gameField1 = localSearchMutation(initialField, gameField1)
            }
            if (gameField2.fitness <= 16) {
                gameField2 = localSearchMutation(initialField, gameField2)
            }
            childrenPool.add(gameField1)
            childrenPool.add(gameField2)
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


fun localSearchMutation(initialField: List<List<Int>>, parent: GameField): GameField {
    var colRepetitionMatrix = getColRepetitionMatrix(initialField, parent.field)
    val fieldChild = parent.field.map { it.toMutableList() }.toMutableList()
    for (i in 0..<(Constants.FIELD_SIZE)) {
        val freeIndices = (0..<(Constants.FIELD_SIZE)).filter { it != i }
        for (freeId in freeIndices) {
            val conjunction = colRepetitionMatrix[i]
                .zip(colRepetitionMatrix[freeId]) { a, b -> a && b }
            val samePosRepetition = conjunction.indexOf(true)
            if (samePosRepetition != -1) {
                val temp = fieldChild[i][samePosRepetition]
                fieldChild[i][samePosRepetition] = fieldChild[freeId][samePosRepetition]
                fieldChild[freeId][samePosRepetition] = temp
                colRepetitionMatrix = getColRepetitionMatrix(initialField, fieldChild)
            }
        }
    }
    return GameField(initialField, fieldChild)
}

private fun getColRepetitionMatrix(initialField: List<List<Int>>, field: List<List<Int>>): List<List<Boolean>> {
    val colRepetitionMatrix = (1..(Constants.FIELD_SIZE))
        .map { MutableList(Constants.FIELD_SIZE) { false } }
        .toMutableList()
    for (j in 0..<(Constants.FIELD_SIZE)) {
        val repetitions = IntArray(Constants.FIELD_SIZE)
        for (i in 0..<(Constants.FIELD_SIZE)) {
            val digit = field[i][j]
            repetitions[digit - 1]++
        }
        for (i in 0..<(Constants.FIELD_SIZE)) {
            if (initialField[i][j] == 0) {
                colRepetitionMatrix[j][i] = repetitions[i] > 1
            }
        }
    }
    return colRepetitionMatrix
}


fun geneticAlgorithm() {
    val initialField = getInitialField()
    var populationState = Population(initialField)
    var generationNumber = 0
    while (true) {
        if (generationNumber++ >= Constants.POPULATION_COUNT) {
            populationState = Population(initialField)
            generationNumber = 0
        }
        val matingPool = populationState.getWeightedPool()
        matingPool.shuffled()
        val matingPopulation = Population(initialField, matingPool)
        val childrenPool = matingPopulation.getChildren()
        val bestGameField = childrenPool.minByOrNull { it.fitness }
        println("$generationNumber Best fitness: ${bestGameField?.fitness}")
//        if (bestGameField?.fitness == 4) {
//            bestGameField.printField()
//        }
        if (bestGameField?.fitness == 0) {
            bestGameField.printField()
            break
        }
        populationState = Population(initialField, childrenPool)
    }
}


fun main() {
    geneticAlgorithm()
//    val initialField = getInitialField()
//    val field = listOf(
//        listOf(8, 3, 6, 5, 1, 2, 9, 4, 7),
//        listOf(1, 7, 5, 6, 2, 8, 4, 9, 3),
//        listOf(6, 2, 4, 8, 3, 7, 1, 9, 5),
//        listOf(3, 7, 9, 4, 8, 6, 2, 5, 1),
//        listOf(2, 6, 3, 7, 5, 1, 8, 4, 8),
//        listOf(9, 5, 8, 2, 4, 9, 7, 3, 2),
//        listOf(8, 1, 7, 1, 6, 4, 3, 5, 9),
//        listOf(4, 8, 1, 9, 7, 3, 6, 8, 6),
//        listOf(5, 4, 2, 3, 9, 5, 1, 2, 8)
//    )
//    val sudokuField = GameField(initialField, field)
//    println(sudokuField.fitness)
//    val newField = localSearchMutation(initialField, sudokuField)
//    newField.printField()
//    println(newField.fitness)
}

