
var finalMsg = "Voici les commandes:\n###DELETE_WORKOUT:id1###\n###DELETE_WORKOUT:id2###\nFin."
var delIdx = finalMsg.indexOf("###DELETE_WORKOUT:")
while (delIdx != -1) {
    val endIdx = finalMsg.indexOf("###", delIdx + 18)
    if (endIdx != -1) {
        val workoutId = finalMsg.substring(delIdx + 18, endIdx).trim()
        println("Found id: $workoutId")
        finalMsg = finalMsg.replaceRange(delIdx, endIdx + 3, "\n[ACTION:DELETE:${workoutId}]\n").trim()
    } else {
        break
    }
    delIdx = finalMsg.indexOf("###DELETE_WORKOUT:")
}
println("Final msg: $finalMsg")

