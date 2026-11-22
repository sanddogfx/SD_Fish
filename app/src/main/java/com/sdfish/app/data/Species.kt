package com.sdfish.app.data

data class Species(
    val id: String,
    val nameRu: String,
    val namePl: String,
    val latin: String,
) {
    val displayName: String get() = "$nameRu ($namePl)"
}

/** Common species in Polish waters. Ids are stable: fishing rules will refer to them. */
object SpeciesCatalog {
    val all: List<Species> = listOf(
        Species("pike", "Щука", "Szczupak", "Esox lucius"),
        Species("perch", "Окунь", "Okoń", "Perca fluviatilis"),
        Species("zander", "Судак", "Sandacz", "Sander lucioperca"),
        Species("bream", "Лещ", "Leszcz", "Abramis brama"),
        Species("roach", "Плотва", "Płoć", "Rutilus rutilus"),
        Species("rudd", "Краснопёрка", "Wzdręga", "Scardinius erythrophthalmus"),
        Species("carp", "Карп", "Karp", "Cyprinus carpio"),
        Species("crucian_carp", "Карась золотой", "Karaś pospolity", "Carassius carassius"),
        Species("prussian_carp", "Карась серебряный", "Karaś srebrzysty", "Carassius gibelio"),
        Species("tench", "Линь", "Lin", "Tinca tinca"),
        Species("catfish", "Сом", "Sum", "Silurus glanis"),
        Species("chub", "Голавль", "Kleń", "Squalius cephalus"),
        Species("asp", "Жерех", "Boleń", "Leuciscus aspius"),
        Species("ide", "Язь", "Jaź", "Leuciscus idus"),
        Species("dace", "Елец", "Jelec", "Leuciscus leuciscus"),
        Species("silver_bream", "Густера", "Krąp", "Blicca bjoerkna"),
        Species("bleak", "Уклейка", "Ukleja", "Alburnus alburnus"),
        Species("ruffe", "Ёрш", "Jazgarz", "Gymnocephalus cernua"),
        Species("gudgeon", "Пескарь", "Kiełb", "Gobio gobio"),
        Species("barbel", "Усач", "Brzana", "Barbus barbus"),
        Species("nase", "Подуст", "Świnka", "Chondrostoma nasus"),
        Species("burbot", "Налим", "Miętus", "Lota lota"),
        Species("eel", "Угорь", "Węgorz", "Anguilla anguilla"),
        Species("grass_carp", "Белый амур", "Amur biały", "Ctenopharyngodon idella"),
        Species("bighead_carp", "Толстолобик", "Tołpyga", "Hypophthalmichthys sp."),
        Species("brown_trout", "Ручьевая форель", "Pstrąg potokowy", "Salmo trutta fario"),
        Species("sea_trout", "Кумжа", "Troć wędrowna", "Salmo trutta trutta"),
        Species("rainbow_trout", "Радужная форель", "Pstrąg tęczowy", "Oncorhynchus mykiss"),
        Species("grayling", "Хариус", "Lipień", "Thymallus thymallus"),
        Species("salmon", "Лосось", "Łosoś", "Salmo salar"),
        Species("cod", "Треска", "Dorsz", "Gadus morhua"),
        Species("flounder", "Камбала", "Flądra", "Platichthys flesus"),
        Species("herring", "Сельдь", "Śledź", "Clupea harengus"),
    )

    fun byId(id: String): Species? = all.firstOrNull { it.id == id }

    /** Text to show for a stored species value. */
    fun displayName(stored: String): String = byId(stored)?.displayName ?: stored

    /** Turns what the user typed or picked into the value to store. */
    fun toStored(text: String): String {
        val t = text.trim()
        return all.firstOrNull {
            it.displayName.equals(t, ignoreCase = true) ||
                it.nameRu.equals(t, ignoreCase = true) ||
                it.namePl.equals(t, ignoreCase = true)
        }?.id ?: t
    }

    fun search(query: String): List<Species> {
        val q = query.trim()
        if (q.isEmpty()) return all
        return all.filter {
            it.displayName.contains(q, ignoreCase = true) || it.latin.contains(q, ignoreCase = true)
        }
    }
}
