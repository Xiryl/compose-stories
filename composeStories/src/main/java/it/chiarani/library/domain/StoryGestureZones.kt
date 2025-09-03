package it.chiarani.library.domain

class EdgeFraction(val value: Float)

data class StoryGestureZones(
    val tapLeftFraction: Float = 0.35f,
    val tapRightFraction: Float = 0.35f,
    val longPressCenterWidth: Float = 0.70f,
    val longPressCenterHeight: Float = 1.00f,
    val swipeLeftEdge: EdgeFraction = EdgeFraction(value = 0.20f),
    val swipeRightEdge: EdgeFraction = EdgeFraction(value = 0.20f),
    val swipeDownEdge: EdgeFraction = EdgeFraction(value = 0.22f)
) {
    init {
        require(tapLeftFraction in 0f..1f && tapRightFraction in 0f..1f)
        require(longPressCenterWidth in 0f..1f && longPressCenterHeight in 0f..1f)
        require(swipeLeftEdge.value in 0f..1f && swipeRightEdge.value in 0f..1f && swipeDownEdge.value in 0f..1f)
    }
}
