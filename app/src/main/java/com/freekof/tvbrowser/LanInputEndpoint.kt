package com.freekof.tvbrowser

object LanInputEndpoint {
    fun qrContent(ipAddress: String, port: Int): String = "http://$ipAddress:$port"
}
