package uz.avtoyordamchi.skaner

/** Eng ko'p uchraydigan umumiy (SAE) xato kodlari — o'zbekcha izoh. */
object Kodlar {

    private val ROYXAT = mapOf(
        "P0100" to "Havo sarfi datchigi (MAF) zanjirida nosozlik",
        "P0101" to "Havo sarfi datchigi (MAF) noto'g'ri ko'rsatyapti",
        "P0102" to "Havo sarfi datchigi (MAF) signali past",
        "P0103" to "Havo sarfi datchigi (MAF) signali yuqori",
        "P0105" to "Kollektor bosimi datchigi (MAP) zanjirida nosozlik",
        "P0106" to "Kollektor bosimi datchigi (MAP) noto'g'ri ko'rsatyapti",
        "P0107" to "Kollektor bosimi datchigi (MAP) signali past",
        "P0108" to "Kollektor bosimi datchigi (MAP) signali yuqori",
        "P0110" to "Kirish havosi harorati datchigi zanjirida nosozlik",
        "P0112" to "Kirish havosi harorati datchigi signali past",
        "P0113" to "Kirish havosi harorati datchigi signali yuqori",
        "P0115" to "Sovutish suyuqligi harorati datchigi zanjirida nosozlik",
        "P0116" to "Sovutish suyuqligi harorati datchigi noto'g'ri ko'rsatyapti",
        "P0117" to "Sovutish suyuqligi harorati datchigi signali past",
        "P0118" to "Sovutish suyuqligi harorati datchigi signali yuqori",
        "P0120" to "Drossel holati datchigi zanjirida nosozlik",
        "P0121" to "Drossel holati datchigi noto'g'ri ko'rsatyapti",
        "P0122" to "Drossel holati datchigi signali past",
        "P0123" to "Drossel holati datchigi signali yuqori",
        "P0125" to "Motor ishchi haroratiga yetib bormayapti (termostat)",
        "P0128" to "Sovutish suyuqligi harorati past — termostat ochiq qolgan bo'lishi mumkin",
        "P0130" to "Kislorod datchigi (1-datchik) zanjirida nosozlik",
        "P0131" to "Kislorod datchigi (1-datchik) kuchlanishi past",
        "P0132" to "Kislorod datchigi (1-datchik) kuchlanishi yuqori",
        "P0133" to "Kislorod datchigi (1-datchik) sekin javob beryapti",
        "P0134" to "Kislorod datchigi (1-datchik) faol emas",
        "P0135" to "Kislorod datchigi (1-datchik) isitgichida nosozlik",
        "P0136" to "Kislorod datchigi (2-datchik) zanjirida nosozlik",
        "P0137" to "Kislorod datchigi (2-datchik) kuchlanishi past",
        "P0138" to "Kislorod datchigi (2-datchik) kuchlanishi yuqori",
        "P0140" to "Kislorod datchigi (2-datchik) faol emas",
        "P0141" to "Kislorod datchigi (2-datchik) isitgichida nosozlik",
        "P0171" to "Aralashma juda kambag'al (havo ko'p, yoqilg'i kam) — ko'pincha havo so'rilishi yoki MAF",
        "P0172" to "Aralashma juda boy (yoqilg'i ko'p) — forsunka, bosim yoki datchik",
        "P0174" to "Aralashma juda kambag'al (2-qator)",
        "P0175" to "Aralashma juda boy (2-qator)",
        "P0201" to "1-forsunka zanjirida nosozlik",
        "P0202" to "2-forsunka zanjirida nosozlik",
        "P0203" to "3-forsunka zanjirida nosozlik",
        "P0204" to "4-forsunka zanjirida nosozlik",
        "P0230" to "Benzin nasosi zanjirida nosozlik",
        "P0300" to "Bir nechta silindrda tutashmaslik (motor 'troit' qilyapti)",
        "P0301" to "1-silindrda tutashmaslik",
        "P0302" to "2-silindrda tutashmaslik",
        "P0303" to "3-silindrda tutashmaslik",
        "P0304" to "4-silindrda tutashmaslik",
        "P0325" to "Detonatsiya datchigi zanjirida nosozlik",
        "P0327" to "Detonatsiya datchigi signali past",
        "P0335" to "Tirsakli val holati datchigi zanjirida nosozlik",
        "P0336" to "Tirsakli val holati datchigi noto'g'ri ko'rsatyapti",
        "P0340" to "Taqsimlash vali holati datchigi zanjirida nosozlik",
        "P0341" to "Taqsimlash vali holati datchigi noto'g'ri ko'rsatyapti",
        "P0351" to "1-g'altak (katushka) zanjirida nosozlik",
        "P0352" to "2-g'altak (katushka) zanjirida nosozlik",
        "P0353" to "3-g'altak (katushka) zanjirida nosozlik",
        "P0354" to "4-g'altak (katushka) zanjirida nosozlik",
        "P0400" to "Chiqindi gazni qaytarish (EGR) tizimida nosozlik",
        "P0401" to "Chiqindi gazni qaytarish (EGR) oqimi yetarli emas",
        "P0420" to "Katalizator samaradorligi past",
        "P0430" to "Katalizator samaradorligi past (2-qator)",
        "P0440" to "Bug'larni ushlash (EVAP) tizimida nosozlik",
        "P0441" to "Bug'larni ushlash (EVAP) tizimida oqim noto'g'ri",
        "P0442" to "Bug'larni ushlash (EVAP) tizimida kichik oqish",
        "P0443" to "Adsorber klapani zanjirida nosozlik",
        "P0446" to "Bug'larni ushlash (EVAP) ventilyatsiya klapani nosozligi",
        "P0455" to "Bug'larni ushlash (EVAP) tizimida katta oqish — benzin qopqog'ini tekshiring",
        "P0480" to "Sovutish ventilyatori (1) boshqaruv zanjirida nosozlik",
        "P0500" to "Tezlik datchigi nosozligi",
        "P0505" to "Salt yurish (xolostoy) boshqaruv tizimida nosozlik",
        "P0506" to "Salt yurish aylanishi kutilganidan past",
        "P0507" to "Salt yurish aylanishi kutilganidan yuqori — havo so'rilishi yoki drossel ifloslangan",
        "P0560" to "Tizim kuchlanishi noto'g'ri",
        "P0562" to "Tizim kuchlanishi past — akkumulyator yoki generator",
        "P0563" to "Tizim kuchlanishi yuqori — generator rele-regulyatori",
        "P0601" to "Motor boshqaruv bloki xotirasida xato",
        "P0602" to "Motor boshqaruv bloki dasturlanmagan",
        "P0606" to "Motor boshqaruv bloki protsessorida nosozlik",
        "P0700" to "Korobka (AKPP) boshqaruv tizimida xato — korobka blokini alohida o'qish kerak",
        "U0001" to "CAN aloqa shinasida nosozlik",
        "U0100" to "Motor boshqaruv bloki bilan aloqa yo'qolgan",
        "U0101" to "Korobka boshqaruv bloki bilan aloqa yo'qolgan",
        "U0121" to "ABS bloki bilan aloqa yo'qolgan",
        "U0140" to "Kuzov boshqaruv bloki bilan aloqa yo'qolgan",
        "U0155" to "Asboblar paneli bilan aloqa yo'qolgan",
    )

    fun izoh(kod: String): String = ROYXAT[kod] ?: when (kod.firstOrNull()) {
        'P' -> "Motor yoki korobka xatosi (bazada izoh yo'q)"
        'C' -> "Shassi xatosi (ABS, rul, osma)"
        'B' -> "Kuzov xatosi (airbag, konditsioner, eshiklar)"
        'U' -> "Bloklar orasidagi aloqa xatosi"
        else -> "Noma'lum kod"
    }

    /** Taxminiy jiddiylik: 2 = qizil, 1 = sariq, 0 = yashil. */
    fun jiddiylik(kod: String): Int = when {
        kod in setOf("P0300", "P0301", "P0302", "P0303", "P0304", "P0335", "P0217", "P0524", "U0100") -> 2
        kod.startsWith("P06") -> 2
        kod in setOf("P0440", "P0441", "P0442", "P0446", "P0455", "P0420", "P0430") -> 0
        else -> 1
    }
}
