package com.ajesh.syncspend.domain.model

enum class TipTone { WATCH, GOOD, INFO }

/** A plain-language observation with a suggestion, shown in Stats' "Tips & suggestions". */
data class Tip(val tone: TipTone, val title: String, val body: String)
