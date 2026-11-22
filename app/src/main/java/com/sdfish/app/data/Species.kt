package com.sdfish.app.data

import com.sdfish.app.AppLanguage

data class Species(
    val id: String,
    val nameRu: String,
    val namePl: String,
    val nameEn: String,
    val latin: String,
) {
    /**
     * Name in the app language. Russian and English keep the Polish name in brackets,
     * because Polish fishing rules use it.
     */
    fun displayName(lang: String = AppLanguage.current()): String = when (lang) {
        "ru" -> "$nameRu ($namePl)"
        "pl" -> namePl
        else -> "$nameEn ($namePl)"
    }

    private val allNames get() = listOf(nameRu, namePl, nameEn, latin)

    fun matches(query: String): Boolean = allNames.any { it.contains(query, ignoreCase = true) } ||
        displayName().contains(query, ignoreCase = true)

    fun isNamed(text: String): Boolean = allNames.any { it.equals(text, ignoreCase = true) } ||
        listOf("ru", "pl", "en").any { displayName(it).equals(text, ignoreCase = true) }
}

/** Common species in Polish waters. Ids are stable: fishing rules will refer to them. */
object SpeciesCatalog {
    val all: List<Species> = listOf(
        Species("pike", "Щука", "Szczupak", "Pike", "Esox lucius"),
        Species("perch", "Окунь", "Okoń", "Perch", "Perca fluviatilis"),
        Species("zander", "Судак", "Sandacz", "Zander", "Sander lucioperca"),
        Species("bream", "Лещ", "Leszcz", "Bream", "Abramis brama"),
        Species("roach", "Плотва", "Płoć", "Roach", "Rutilus rutilus"),
        Species("rudd", "Краснопёрка", "Wzdręga", "Rudd", "Scardinius erythrophthalmus"),
        Species("carp", "Карп", "Karp", "Carp", "Cyprinus carpio"),
        Species("crucian_carp", "Карась золотой", "Karaś pospolity", "Crucian carp", "Carassius carassius"),
        Species("prussian_carp", "Карась серебряный", "Karaś srebrzysty", "Prussian carp", "Carassius gibelio"),
        Species("tench", "Линь", "Lin", "Tench", "Tinca tinca"),
        Species("catfish", "Сом", "Sum", "Wels catfish", "Silurus glanis"),
        Species("chub", "Голавль", "Kleń", "Chub", "Squalius cephalus"),
        Species("asp", "Жерех", "Boleń", "Asp", "Leuciscus aspius"),
        Species("ide", "Язь", "Jaź", "Ide", "Leuciscus idus"),
        Species("dace", "Елец", "Jelec", "Dace", "Leuciscus leuciscus"),
        Species("silver_bream", "Густера", "Krąp", "Silver bream", "Blicca bjoerkna"),
        Species("bleak", "Уклейка", "Ukleja", "Bleak", "Alburnus alburnus"),
        Species("ruffe", "Ёрш", "Jazgarz", "Ruffe", "Gymnocephalus cernua"),
        Species("gudgeon", "Пескарь", "Kiełb", "Gudgeon", "Gobio gobio"),
        Species("barbel", "Усач", "Brzana", "Barbel", "Barbus barbus"),
        Species("nase", "Подуст", "Świnka", "Nase", "Chondrostoma nasus"),
        Species("burbot", "Налим", "Miętus", "Burbot", "Lota lota"),
        Species("eel", "Угорь", "Węgorz", "Eel", "Anguilla anguilla"),
        Species("grass_carp", "Белый амур", "Amur biały", "Grass carp", "Ctenopharyngodon idella"),
        Species("bighead_carp", "Толстолобик", "Tołpyga", "Bighead carp", "Hypophthalmichthys sp."),
        Species("brown_trout", "Ручьевая форель", "Pstrąg potokowy", "Brown trout", "Salmo trutta fario"),
        Species("sea_trout", "Кумжа", "Troć wędrowna", "Sea trout", "Salmo trutta trutta"),
        Species("rainbow_trout", "Радужная форель", "Pstrąg tęczowy", "Rainbow trout", "Oncorhynchus mykiss"),
        Species("grayling", "Хариус", "Lipień", "Grayling", "Thymallus thymallus"),
        Species("salmon", "Лосось", "Łosoś", "Atlantic salmon", "Salmo salar"),
        Species("cod", "Треска", "Dorsz", "Cod", "Gadus morhua"),
        Species("flounder", "Камбала", "Flądra", "Flounder", "Platichthys flesus"),
        Species("herring", "Сельдь", "Śledź", "Herring", "Clupea harengus"),
    )

    fun byId(id: String): Species? = all.firstOrNull { it.id == id }

    /** Text to show for a stored species value. */
    fun displayName(stored: String): String = byId(stored)?.displayName() ?: stored

    /** Turns what the user typed or picked into the value to store. */
    fun toStored(text: String): String {
        val t = text.trim()
        return all.firstOrNull { it.isNamed(t) }?.id ?: t
    }

    fun search(query: String): List<Species> {
        val q = query.trim()
        if (q.isEmpty()) return all
        return all.filter { it.matches(q) }
    }
}
