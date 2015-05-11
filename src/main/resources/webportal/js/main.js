var map;

function map_initialize() {
    var mapOptions = {
        center: {lat: 48.157654, lng: 17.069182},
        zoom: 15,
        draggableCursor: 'crosshair'
    };
    map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
}
google.maps.event.addDomListener(window, 'load', map_initialize);

function fetch_location() {
    $.get("/api/vehicle/39", null, function(data) {
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

var lat;
var lon;
var mapMarkers = [];
var infoWindows = [];
var lastInfowindow = null;

var infoWindowContent = [];
var infowindow = new google.maps.InfoWindow();

$(document).ready(function() {
    //fetch_location();
    //map_initialize();    
    $.post("/getPoi", null, function(data) {
        var html = "";
        var poiArray = data.poiList;
        for (var i in poiArray) {
            var poi = poiArray[i];
            html += "<tr>";
            html += "<td>" + (i + 1) + ".</td>";
            html += "<td><strong><a href=\"#\" class=\"poi-name\" data-poi-id=\"" + i + "\">" + poi.title + "</a></strong></td>";
            html += "<td>" + poi.filePath + "</td>";
            html += "<td>" + poi.lat.toFixed(5) + "</td>";
            html += "<td>" + poi.lon.toFixed(5) + "</td>";
            html += "<td>" + poi.radius + "</td>";
            html += "<td><a href=\"#\" data-poi-goto=\"" + i + "\">Show on map</a>";
            html += "</tr>";

            var myLatlng = new google.maps.LatLng(poi.lat, poi.lon);
            var marker = new google.maps.Marker({
                position: myLatlng,
                map: map,
                title: poi.title
            });

            mapMarkers[i] = marker;

            infoWindowContent[i] = '<div id="content">' +
                    '<div id="siteNotice">' +
                    '</div>' +
                    '<h4 id="firstHeading" class="firstHeading">' + poi.title + '</h4>' +
                    '</div>' +
                    '<button type="button" onclick="editPoi(' + poi.id + ');" class="btn btn-default" data-dismiss="modal">Upraviť</button>' +
                    '<button type="button" onclick="deletePoi(' + poi.id + ');" class="btn btn-default" data-dismiss="modal">Zmazať</button>';

            /*
             infoWindows[i] = new google.maps.InfoWindow({
             content: contentString
             });
             */
            /*
             google.maps.event.addListener(mapMarkers[i], 'click', function() {
             infowindow[i].open(map, mapMarkers[i]);
             });
             */

            google.maps.event.addListener(mapMarkers[i], 'click', function(innerKey) {
                return function() {
                    infowindow.setContent(infoWindowContent[innerKey]);
                    infowindow.open(map, mapMarkers[innerKey]);
                }
            }(i));
        }



        google.maps.event.addListener(map, 'click', function(event) {
            //alert('lat: ' + event.latLng.lat() + ' lon: ' + event.latLng.lng());
            lat = event.latLng.lat();
            lon = event.latLng.lng();
            $('#myModal').modal('show');
            document.getElementById('myModalLabel').innerHTML = '<strong>Vytvoriť bod záujmu pre pozíciu:</strong> ' + event.latLng.lat().toFixed(5) + ', ' + event.latLng.lng().toFixed(5);
            $('#myAlert').hide();
            //document.getElementById('myAlert').hide();
        });


        //map.setCenter(mapMarkers[1].getPositions());
        $("#poiTable tbody").html(html);
        $("#poiTable tbody a.poi-name").editable({
            type: "text",
            placement: "right",
            success: function(response, newValue) {
                var message = "<strong>Ty pan! </strong> Si zmeniu idecku " + $(this).attr("data-poi-id") + " nejm na <i>" + newValue + "</i>";
                $("#testAlert").html(message);
            }
        });

        $("#poiTable tbody a[data-poi-goto]").click(function() {
            var num = $(this).attr("data-poi-goto");
            map.setCenter(mapMarkers[num].getPosition());
            infowindow.setContent(infoWindowContent[num]);
            infowindow.open(map, mapMarkers[num]);
        });

    }, "json");
});

function doSubmit() {
    $.ajax({
        url: 'http://bpbp.ctrgn.net/savePoi', //this is the submit URL
        type: 'POST', //or POST
        data: $('#myForm').serialize() + "&lat=" + lat + "&lon=" + lon,
        success: function(data) {
            alert('successfully submitted')

        }
    });
    $('#myAlert').fadeIn();
}

function reloadPage() {
    location.reload();
}

function editPoi(id) {
    alert("edit" + id);
}

function deletePoi(id) {
    alert("delete" + id);
}

