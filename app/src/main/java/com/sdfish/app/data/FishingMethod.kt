package com.sdfish.app.data

import com.sdfish.app.AppLanguage

data class FishingMethod(val id: String, val nameRu: String, val nameBe: String, val namePl: String, val nameEn: String) {
    fun displayName(lang: String = AppLanguage.current()): String = when (lang) {
        "ru" -> nameRu
        "be" -> nameBe
        "pl" -> namePl
        else -> nameEn
    }
}

/** Fishing methods. Ids are stable: they are stored in the database. */
object FishingMethods {
    val all: List<FishingMethod> = listOf(
        FishingMethod("spinning", "Спиннинг", "Спінінг", "Spinning", "Spinning"),
        FishingMethod("float", "Поплавок", "Паплавок", "Spławik", "Float"),
        FishingMethod("feeder", "Фидер", "Фідар", "Feeder", "Feeder"),
        FishingMethod("bottom", "Донка", "Донка", "Grunt", "Ledgering"),
        FishingMethod("carp", "Карповая ловля", "Карпавая лоўля", "Karpiowanie", "Carp fishing"),
        FishingMethod("fly", "Нахлыст", "Нахлыст", "Mucha", "Fly fishing"),
        FishingMethod("trolling", "Троллинг", "Тролінг", "Trolling", "Trolling"),
        FishingMethod("ice", "Зимняя ловля", "Зімовая лоўля", "Podlodowe", "Ice fishing"),
        FishingMethod("other", "Другое", "Іншае", "Inne", "Other"),
    )

    fun byId(id: String?): FishingMethod? = all.firstOrNull { it.id == id }

    fun displayName(id: String?): String = byId(id)?.displayName() ?: id.orEmpty()
}
