package com.sdfish.app.data

import com.sdfish.app.AppLanguage

data class Species(
    val id: String,
    val nameRu: String,
    val nameBe: String,
    val namePl: String,
    val nameEn: String,
    val latin: String,
) {
    /** Name in the app language only. */
    fun displayName(lang: String = AppLanguage.current()): String = when (lang) {
        "ru" -> nameRu
        "be" -> nameBe
        "pl" -> namePl
        else -> nameEn
    }

    private val allNames get() = listOf(nameRu, nameBe, namePl, nameEn, latin)

    /** Search finds a fish by its name in any language, so a Polish name works in every language. */
    fun matches(query: String): Boolean = allNames.any { it.contains(query, ignoreCase = true) }

    fun isNamed(text: String): Boolean = allNames.any { it.equals(text, ignoreCase = true) } ||
        // Names shown by older versions, such as "Щука (Szczupak)".
        listOf(nameRu, nameEn).any { "$it ($namePl)".equals(text, ignoreCase = true) }
}

/** Common species in Polish waters. Ids are stable: fishing rules will refer to them. */
object SpeciesCatalog {
    val all: List<Species> = listOf(
        Species("pike", "Щука", "Шчупак", "Szczupak", "Pike", "Esox lucius"),
        Species("perch", "Окунь", "Акунь", "Okoń", "Perch", "Perca fluviatilis"),
        Species("zander", "Судак", "Судак", "Sandacz", "Zander", "Sander lucioperca"),
        Species("bream", "Лещ", "Лешч", "Leszcz", "Bream", "Abramis brama"),
        Species("roach", "Плотва", "Плотка", "Płoć", "Roach", "Rutilus rutilus"),
        Species("rudd", "Краснопёрка", "Чырванапёрка", "Wzdręga", "Rudd", "Scardinius erythrophthalmus"),
        Species("carp", "Карп", "Карп", "Karp", "Carp", "Cyprinus carpio"),
        Species("crucian_carp", "Карась золотой", "Карась залаты", "Karaś pospolity", "Crucian carp", "Carassius carassius"),
        Species("prussian_carp", "Карась серебряный", "Карась серабрысты", "Karaś srebrzysty", "Prussian carp", "Carassius gibelio"),
        Species("tench", "Линь", "Лінь", "Lin", "Tench", "Tinca tinca"),
        Species("catfish", "Сом", "Сом", "Sum", "Wels catfish", "Silurus glanis"),
        Species("chub", "Голавль", "Галавень", "Kleń", "Chub", "Squalius cephalus"),
        Species("asp", "Жерех", "Жэрах", "Boleń", "Asp", "Leuciscus aspius"),
        Species("ide", "Язь", "Язь", "Jaź", "Ide", "Leuciscus idus"),
        Species("dace", "Елец", "Ялец", "Jelec", "Dace", "Leuciscus leuciscus"),
        Species("silver_bream", "Густера", "Гусцяра", "Krąp", "Silver bream", "Blicca bjoerkna"),
        Species("bleak", "Уклейка", "Уклейка", "Ukleja", "Bleak", "Alburnus alburnus"),
        Species("ruffe", "Ёрш", "Ёрш", "Jazgarz", "Ruffe", "Gymnocephalus cernua"),
        Species("gudgeon", "Пескарь", "Пячкур", "Kiełb", "Gudgeon", "Gobio gobio"),
        Species("barbel", "Усач", "Вусач", "Brzana", "Barbel", "Barbus barbus"),
        Species("nase", "Подуст", "Падуст", "Świnka", "Nase", "Chondrostoma nasus"),
        Species("burbot", "Налим", "Мянтуз", "Miętus", "Burbot", "Lota lota"),
        Species("eel", "Угорь", "Вугор", "Węgorz", "Eel", "Anguilla anguilla"),
        Species("grass_carp", "Белый амур", "Белы амур", "Amur biały", "Grass carp", "Ctenopharyngodon idella"),
        Species("bighead_carp", "Толстолобик", "Таўсталобік", "Tołpyga", "Bighead carp", "Hypophthalmichthys sp."),
        Species("brown_trout", "Ручьевая форель", "Ручаёвая стронга", "Pstrąg potokowy", "Brown trout", "Salmo trutta fario"),
        Species("sea_trout", "Кумжа", "Кумжа", "Troć wędrowna", "Sea trout", "Salmo trutta trutta"),
        Species("rainbow_trout", "Радужная форель", "Вясёлкавая стронга", "Pstrąg tęczowy", "Rainbow trout", "Oncorhynchus mykiss"),
        Species("grayling", "Хариус", "Харыус", "Lipień", "Grayling", "Thymallus thymallus"),
        Species("salmon", "Лосось", "Ласось", "Łosoś", "Atlantic salmon", "Salmo salar"),
        Species("cod", "Треска", "Траска", "Dorsz", "Cod", "Gadus morhua"),
        Species("flounder", "Камбала", "Камбала", "Flądra", "Flounder", "Platichthys flesus"),
        Species("herring", "Сельдь", "Селядзец", "Śledź", "Herring", "Clupea harengus"),
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
