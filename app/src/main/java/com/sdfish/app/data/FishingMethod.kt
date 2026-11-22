package com.sdfish.app.data

import com.sdfish.app.AppLanguage

data class FishingMethod(val id: String, val nameRu: String, val namePl: String, val nameEn: String) {
    fun displayName(lang: String = AppLanguage.current()): String = when (lang) {
        "ru" -> nameRu
        "pl" -> namePl
        else -> nameEn
    }
}

/** Fishing methods. Ids are stable: they are stored in the database. */
object FishingMethods {
    val all: List<FishingMethod> = listOf(
        FishingMethod("spinning", "Спиннинг", "Spinning", "Spinning"),
        FishingMethod("float", "Поплавок", "Spławik", "Float"),
        FishingMethod("feeder", "Фидер", "Feeder", "Feeder"),
        FishingMethod("bottom", "Донка", "Grunt", "Ledgering"),
        FishingMethod("carp", "Карповая ловля", "Karpiowanie", "Carp fishing"),
        FishingMethod("fly", "Нахлыст", "Mucha", "Fly fishing"),
        FishingMethod("trolling", "Троллинг", "Trolling", "Trolling"),
        FishingMethod("ice", "Зимняя ловля", "Podlodowe", "Ice fishing"),
        FishingMethod("other", "Другое", "Inne", "Other"),
    )

    fun byId(id: String?): FishingMethod? = all.firstOrNull { it.id == id }

    fun displayName(id: String?): String = byId(id)?.displayName() ?: id.orEmpty()
}
