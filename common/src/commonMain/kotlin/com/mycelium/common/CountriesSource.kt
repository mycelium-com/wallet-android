package com.mycelium.common

/**
 * Utility object providing country data and models for internationalization support.
 * Contains country codes, names, and associated metadata for use in gift box and other features.
 */
object CountriesSource {
    // Countries list harvested from https://github.com/Dinuks/country-nationality-list/blob/master/countries.json
    private val countryCodes =
        "TJ JM HT ST MS AE PK NL LU BZ IR BO UY GH SA CI MF TF AI QA SX LY BV PG KG GQ EH NU PR GD KR HM SM SL CD MK TR DZ GE PS BB UA GP PF NA BW SY TG DO AQ CH MG FO VG GI BN LA IS EE UM LT RS MR AD HU TK MY AO CV NF PA GW BE PT GB IM US YE HK AZ CC ML SK VU TL HR SR MU CZ PM LS WS KM IT BI WF GN SG CO CN AW MA FI VA ZW KY BH PY EC LR RU PL OM MT SS DE TM SJ MM TT IL BD NR LK UG NG BQ MX CW SI MN CA AX VN TW JP IO RO BG GU BR AM ZM DJ JE AT CM SE FJ KZ GL GY CX MW TN ZA TO CY MV PN RW NI KN BJ ET GM TZ VC FK SD MC AU CL DK FR TC CU AL MZ BS NE GT LI NP BF PW KW IN GA TV MO SH MD CK AR SC IE ES LB BM RE KI AG MQ SV JO TH SO MH CG KP GF BA YT GS KE PE BT SZ CR TD DM NC GR GG HN VI CF SN AF MP PH BY LV NO EG KH IQ LC NZ BL UZ ID ER VE FM SB ME AS".split(
            " "
        )

    val countryModels by lazy {
        countryCodes.map { code ->
            val displayName = getCountryDisplayName(code)
            CountryModel(
                name = displayName,
                acronym = code,
                acronym3 = code, // Using 2-letter code as fallback for 3-letter code
                code = 0, // Numeric country code not available in current implementation
                nationality = displayName,
            )
        }.sortedBy { it.name }
    }
}