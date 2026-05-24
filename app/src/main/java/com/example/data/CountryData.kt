package com.example.data

data class CountryInfo(
    val name: String,
    val lifeExpectancy: Double,
    val flagEmoji: String
)

object CountryData {
    val countries = listOf(
        CountryInfo("Global Average", 73.0, "🌐"),
        CountryInfo("Afghanistan", 62.0, "🇦🇫"),
        CountryInfo("Australia", 83.0, "🇦🇺"),
        CountryInfo("Bangladesh", 73.0, "🇧🇩"),
        CountryInfo("Brazil", 73.0, "🇧🇷"),
        CountryInfo("Canada", 82.0, "🇨🇦"),
        CountryInfo("Central African Republic", 54.0, "🇨🇫"),
        CountryInfo("China", 78.0, "🇨🇳"),
        CountryInfo("Egypt", 70.0, "🇪🇬"),
        CountryInfo("France", 82.0, "🇫🇷"),
        CountryInfo("Germany", 81.0, "🇩🇪"),
        CountryInfo("India", 68.0, "🇮🇳"),
        CountryInfo("Indonesia", 68.0, "🇮🇩"),
        CountryInfo("Italy", 83.0, "🇮🇹"),
        CountryInfo("Japan", 85.0, "🇯🇵"),
        CountryInfo("Kenya", 62.0, "🇰🇪"),
        CountryInfo("Mexico", 72.0, "🇲🇽"),
        CountryInfo("Monaco", 89.0, "🇲🇨"),
        CountryInfo("Nigeria", 53.0, "🇳🇬"),
        CountryInfo("Pakistan", 66.0, "🇵🇰"),
        CountryInfo("Russia", 70.0, "🇷🇺"),
        CountryInfo("Singapore", 84.0, "🇸🇬"),
        CountryInfo("South Africa", 62.0, "🇿🇦"),
        CountryInfo("South Korea", 84.0, "🇰🇷"),
        CountryInfo("Spain", 83.0, "🇪🇸"),
        CountryInfo("Switzerland", 84.0, "🇨🇭"),
        CountryInfo("Turkey", 76.0, "🇹🇷"),
        CountryInfo("United Kingdom", 81.0, "🇬🇧"),
        CountryInfo("United States", 77.0, "🇺🇸")
    ).sortedBy { it.name }

    fun getByCountryName(name: String): CountryInfo {
        return countries.firstOrNull { it.name == name } ?: countries[0]
    }
}
