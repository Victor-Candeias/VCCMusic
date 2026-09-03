package pt.vcc.vccmusic.domain.model

@JvmInline
value class TrackId(val value: String) {
    init {
        require(value.isNotBlank()) { "Track ID cannot be blank" }
    }
}

