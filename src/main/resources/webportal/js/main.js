var map;
var mapMarkers = [];

function map_initialize() {
    var mapOptions = {
        center: {lat: -34.397, lng: 150.644},
        zoom: 8
    };
    map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
}
google.maps.event.addDomListener(window, 'load', map_initialize);

function fetch_location() {
    $.get("/api/vehicle/39", null, function(data) {
        var coords = data.coordinates;
        var myLatlng = new google.maps.LatLng(coords.latitude, coords.longitude);
        var marker = new google.maps.Marker({
            position: myLatlng,
            map: map,
            title: "Buseus 39 locationis ala " + coords.time
        });
        map.setCenter(marker.getPosition());
        setTimeout(function() {
            fetch_location();
        }, 1500);
    }, "json");
}

$(document).ready(function() {
    fetch_location();
    $.get("/api/getPoi", null, function(data) {
        var html = "";
        var poiArray = data.poiList;
        for (var i in poiArray) {
            var poi = poiArray[i];
            html += "<tr>";
            html += "<td>" + poi.id + "</td>";
            html += "<td><strong><a href=\"#\" class=\"poi-name\" data-poi-id=\"" + i + "\">" + poi.name + "</a></strong></td>";
            html += "<td>" + poi.latitude + "</td>";
            html += "<td>" + poi.longitude + "</td>";
            html += "<td>" + poi.accuracy + "</td>";
            html += "<td>" + poi.time + "</td>";
            html += "<td><a href=\"#\" data-poi-goto=\"" + i + "\">Go To!</a>";
            html += "</tr>";

            var myLatlng = new google.maps.LatLng(poi.latitude, poi.longitude);
            var marker = new google.maps.Marker({
                position: myLatlng,
                map: map,
                title: poi.name
            });
            mapMarkers[i] = marker;
        }
        $("#testTable tbody").html(html);
        $("#testTable tbody a.poi-name").editable({
            type: "text",
            placement: "right",
            success: function(response, newValue) {
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