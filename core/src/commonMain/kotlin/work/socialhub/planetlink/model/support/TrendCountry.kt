package net.socialhub.planetlink.model.support

class TrendCountry {
    //region // Getter&Setter
    var id: Int? = null

    var name: String? = null

    //endregion
    var locations: List<TrendLocation>? = null

    class TrendLocation {
        //region // Getter&Setter
        var id: Int? = null

        //endregion
        var name: String? = null
    }
}
