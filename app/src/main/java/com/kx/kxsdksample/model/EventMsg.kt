package com.kx.kxsdksample.model

enum class EventType {
    HeadsetPlugState
}

/**
 * EventBus事件消息
 */
class EventMsg(val type:EventType, val value:Int){
    companion object {
        fun getInstance(type:EventType, value:Int): EventMsg {
            return EventMsg(type, value)
        }
    }
}
