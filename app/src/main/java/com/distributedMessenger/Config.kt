package com.distributedMessenger

import android.content.Context
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser

object Config {
    lateinit var logDir: String
    lateinit var dbName: String
    var dbVersion: Int = 1

    fun initialize(context: Context) {
        context.resources.getXml(R.xml.app_config).use { parser ->
            while (parser.eventType != XmlResourceParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    parser.next()
                    continue
                }
                when (parser.name) {
                    "logdir" -> logDir = parser.getAttributeValue(null, "dir")
                    "database" -> {
                        dbName = parser.getAttributeValue(null, "name")
                        dbVersion = parser.getAttributeValue(null, "version")?.toIntOrNull() ?: 1
                    }
                }
                parser.next()
            }
        }
    }
}
