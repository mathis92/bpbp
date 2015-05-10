var map;
var mapMarkers = [];

function map_initialize() {
    var mapOptions = {
        center: {lat: 48.157654, lng: 17.069182},
        zoom: 15
    };
    map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);

}
google.maps.event.addDomListener(window, 'load', map_initialize);


function fetch_location() {
    $.get("/api/vehicle/39", null, function (data) {
        var coords = data.positionList;
        var myLatlng = new google.maps.LatLng(coords.latitude, coords.longitude);
        var marker = new google.maps.Marker({
            position: myLatlng,
            map: map,
            title: "Buseus 39 locationis ala " + coords.time
        });
        map.setCenter(marker.getPosition());
        /*
         setTimeout(function() {
         fetch_location();
         }, 1500);
         */
    }, "json");
}



$(document).ready(function () {
    //fetch_location();
    //map_initialize();
    $.get("/api/vehicle/getPoi", null, function (data) {
        var html = "";
        var poiArray = data.poiList;
        for (var i in poiArray) {
            var poi = poiArray[i];
            html += "<tr>";
            html += "<td>" + poi.id + "</td>";
            html += "<td><strong><a href=\"#\" class=\"poi-name\" data-poi-id=\"" + i + "\">" + poi.title + "</a></strong></td>";
            html += "<td>" + poi.lat + "</td>";
            html += "<td>" + poi.lon + "</td>";
            html += "<td>" + poi.radius + "</td>";
            html += "<td>" + poi.filePath + "</td>";
            html += "<td><a href=\"#\" data-poi-goto=\"" + i + "\">Go To!</a>";
            html += "</tr>";

            var myLatlng = new google.maps.LatLng(poi.lat, poi.lon);
            var marker = new google.maps.Marker({
                position: myLatlng,
                map: map,
                title: poi.title
            });
            mapMarkers[i] = marker;

            var contentString = '<div id="content">' +
                    '<div id="siteNotice">' +
                    '</div>' +
                    '<h1 id="firstHeading" class="firstHeading">'+ poi.title +'</h1>' +
                    '</div>';

            var infowindow = new google.maps.InfoWindow({
                content: contentString
            });

           
            google.maps.event.addListener(marker, 'click', function () {
                infowindow.open(map, marker);
            });
        }


        
        //map.setCenter(mapMarkers[1].getPositions());
        $("#testTable tbody").html(html);
        $("#testTable tbody a.poi-name").editable({
            type: "text",
            placement: "right",
            success: function (response, newValue) {
                var message = "<strong>Ty pan! </strong> Si zmeniu idecku " + $(this).attr("data-poi-id") + " nejm na <i>" + newValue + "</i>";
                $("#testAlert").html(message);
            }
        });
        /*
         $("#testTable tbody a[data-poi-goto]").click(function() {
         map.setCenter(mapMarkers[$(this).attr("data-poi-goto")].getPosition());
         });
         */
    }, "json");
});